#!/bin/bash
# =============================================================================
# Predictify Backend - Script de Backup de Base de Datos
# =============================================================================
# Uso: ./deploy/backup.sh
# =============================================================================

set -e

# Configuración
PROJECT_DIR="/opt/predictify-backend"
BACKUP_DIR="$PROJECT_DIR/backups"
RETENTION_DAYS=7

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

cd "$PROJECT_DIR"

# Cargar variables de entorno
source .env

# Nombre del archivo con timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/predictify_db_$TIMESTAMP.sql.gz"

log_info "Iniciando backup de base de datos..."

# Crear directorio si no existe
mkdir -p "$BACKUP_DIR"

# Realizar backup
docker compose -f docker-compose.prod.yml exec -T db \
    pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > "$BACKUP_FILE"

# Verificar que el backup se creó correctamente
if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    log_info "✅ Backup creado: $BACKUP_FILE ($SIZE)"
else
    log_warn "❌ Error al crear backup"
    exit 1
fi

# Limpiar backups antiguos
log_info "Limpiando backups antiguos (más de $RETENTION_DAYS días)..."
find "$BACKUP_DIR" -name "predictify_db_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Listar backups disponibles
echo ""
log_info "Backups disponibles:"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null || echo "  (ninguno)"
