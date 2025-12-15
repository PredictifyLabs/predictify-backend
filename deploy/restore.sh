#!/bin/bash
# =============================================================================
# Predictify Backend - Script de Restauración de Base de Datos
# =============================================================================
# Uso: ./deploy/restore.sh [archivo_backup.sql.gz]
# =============================================================================

set -e

# Configuración
PROJECT_DIR="/opt/predictify-backend"
BACKUP_DIR="$PROJECT_DIR/backups"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

cd "$PROJECT_DIR"
source .env

# Si no se especifica archivo, mostrar disponibles
if [ -z "$1" ]; then
    echo "Backups disponibles:"
    echo ""
    ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null || echo "  (ninguno)"
    echo ""
    echo "Uso: ./deploy/restore.sh <nombre_archivo.sql.gz>"
    exit 1
fi

BACKUP_FILE="$1"

# Verificar si es ruta relativa
if [ ! -f "$BACKUP_FILE" ]; then
    BACKUP_FILE="$BACKUP_DIR/$1"
fi

if [ ! -f "$BACKUP_FILE" ]; then
    log_error "Archivo no encontrado: $1"
    exit 1
fi

# Confirmar restauración
echo ""
log_warn "⚠️  ADVERTENCIA: Esto sobrescribirá TODOS los datos actuales"
echo "Archivo: $BACKUP_FILE"
echo ""
read -p "¿Estás seguro? (escribe 'SI' para confirmar): " CONFIRM

if [ "$CONFIRM" != "SI" ]; then
    log_info "Restauración cancelada"
    exit 0
fi

log_info "Iniciando restauración..."

# Detener la API para evitar conexiones
docker compose -f docker-compose.prod.yml stop api

# Restaurar
log_info "Restaurando base de datos..."
gunzip -c "$BACKUP_FILE" | docker compose -f docker-compose.prod.yml exec -T db \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"

# Reiniciar API
docker compose -f docker-compose.prod.yml start api

log_info "✅ Restauración completada"
log_info "Esperando a que la API esté lista..."

sleep 10

if curl -sf http://localhost:8081/actuator/health | grep -q "UP"; then
    log_info "✅ API funcionando correctamente"
else
    log_warn "La API puede tardar unos segundos más en iniciar"
fi
