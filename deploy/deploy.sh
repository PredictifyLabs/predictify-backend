#!/bin/bash
# =============================================================================
# Predictify Backend - Script de Despliegue/Actualización
# =============================================================================
# Uso: ./deploy/deploy.sh [--build] [--logs]
# =============================================================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Directorio del proyecto
PROJECT_DIR="/opt/predictify-backend"
cd "$PROJECT_DIR"

# Parsear argumentos
BUILD_FLAG=""
SHOW_LOGS=false

for arg in "$@"; do
    case $arg in
        --build)
            BUILD_FLAG="--build"
            ;;
        --logs)
            SHOW_LOGS=true
            ;;
    esac
done

echo ""
echo "=========================================="
log_info "🚀 Desplegando Predictify Backend"
echo "=========================================="
echo ""

# -----------------------------------------------------------------------------
# 1. Verificar requisitos
# -----------------------------------------------------------------------------
log_step "Verificando requisitos..."

if [ ! -f ".env" ]; then
    log_error "Archivo .env no encontrado!"
    log_info "Ejecuta: cp .env.example .env && nano .env"
    exit 1
fi

if [ ! -f "docker-compose.prod.yml" ]; then
    log_error "docker-compose.prod.yml no encontrado!"
    exit 1
fi

# Verificar variables críticas
source .env
if [ -z "$POSTGRES_PASSWORD" ] || [ "$POSTGRES_PASSWORD" = "TU_CONTRASEÑA_SEGURA_AQUI" ]; then
    log_error "POSTGRES_PASSWORD no está configurado en .env"
    exit 1
fi

if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "TU_CLAVE_JWT_SEGURA_DE_64_CARACTERES_MINIMO_AQUI" ]; then
    log_error "JWT_SECRET no está configurado en .env"
    log_info "Genera uno con: openssl rand -hex 32"
    exit 1
fi

log_info "✓ Requisitos verificados"

# -----------------------------------------------------------------------------
# 2. Obtener últimos cambios (si es un repo git)
# -----------------------------------------------------------------------------
if [ -d ".git" ]; then
    log_step "Obteniendo últimos cambios de Git..."
    git fetch origin
    
    LOCAL=$(git rev-parse HEAD)
    REMOTE=$(git rev-parse @{u} 2>/dev/null || echo "")
    
    if [ -n "$REMOTE" ] && [ "$LOCAL" != "$REMOTE" ]; then
        log_info "Nuevos cambios detectados, actualizando..."
        git pull origin main
        BUILD_FLAG="--build"
    else
        log_info "✓ Código actualizado"
    fi
fi

# -----------------------------------------------------------------------------
# 3. Crear backup antes de actualizar (si hay datos)
# -----------------------------------------------------------------------------
if docker compose -f docker-compose.prod.yml ps db 2>/dev/null | grep -q "Up"; then
    log_step "Creando backup de base de datos..."
    ./deploy/backup.sh || log_warn "No se pudo crear backup (puede ser primera ejecución)"
fi

# -----------------------------------------------------------------------------
# 4. Desplegar servicios
# -----------------------------------------------------------------------------
log_step "Desplegando servicios Docker..."

# Detener servicios existentes gracefully
docker compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true

# Limpiar imágenes huérfanas (liberar espacio)
docker image prune -f 2>/dev/null || true

# Iniciar servicios
docker compose -f docker-compose.prod.yml up -d $BUILD_FLAG

# -----------------------------------------------------------------------------
# 5. Esperar a que los servicios estén listos
# -----------------------------------------------------------------------------
log_step "Esperando a que los servicios estén listos..."

# Esperar a la base de datos
echo -n "  Base de datos: "
for i in {1..30}; do
    if docker compose -f docker-compose.prod.yml exec -T db pg_isready -U postgres &>/dev/null; then
        echo -e "${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# Esperar a la API
echo -n "  API: "
for i in {1..60}; do
    if curl -sf http://localhost:8081/actuator/health &>/dev/null; then
        echo -e "${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 3
done

# -----------------------------------------------------------------------------
# 6. Verificar estado
# -----------------------------------------------------------------------------
log_step "Verificando estado de servicios..."
echo ""
docker compose -f docker-compose.prod.yml ps

# Health check final
echo ""
if curl -sf http://localhost:8081/actuator/health | grep -q "UP"; then
    log_info "✅ API funcionando correctamente"
    
    # Mostrar información útil
    echo ""
    echo "=========================================="
    echo "  Endpoints disponibles:"
    echo "  - Health: http://localhost:8081/actuator/health"
    echo "  - Swagger: http://localhost:8081/swagger-ui.html"
    echo "=========================================="
else
    log_error "❌ La API no responde correctamente"
    log_info "Revisa los logs con: docker compose -f docker-compose.prod.yml logs api"
    exit 1
fi

# Mostrar logs si se solicitó
if [ "$SHOW_LOGS" = true ]; then
    echo ""
    log_info "Mostrando logs (Ctrl+C para salir)..."
    docker compose -f docker-compose.prod.yml logs -f api
fi
