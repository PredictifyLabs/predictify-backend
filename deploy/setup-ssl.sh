#!/bin/bash
# =============================================================================
# Predictify Backend - Configurar SSL con Let's Encrypt
# =============================================================================
# Uso: ./deploy/setup-ssl.sh tu-dominio.com [email@ejemplo.com]
# =============================================================================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Verificar argumentos
if [ -z "$1" ]; then
    log_error "Uso: ./deploy/setup-ssl.sh <dominio> [email]"
    echo "  Ejemplo: ./deploy/setup-ssl.sh api.predictify.com admin@predictify.com"
    exit 1
fi

DOMAIN=$1
EMAIL=${2:-"admin@$DOMAIN"}
PROJECT_DIR="/opt/predictify-backend"
NGINX_DIR="$PROJECT_DIR/deploy/nginx"

log_info "=== Configurando SSL para $DOMAIN ==="

# -----------------------------------------------------------------------------
# 1. Verificar que el dominio apunta a este servidor
# -----------------------------------------------------------------------------
log_info "Verificando DNS..."
SERVER_IP=$(curl -s ifconfig.me)
DOMAIN_IP=$(dig +short $DOMAIN | head -1)

if [ "$SERVER_IP" != "$DOMAIN_IP" ]; then
    log_warn "El dominio $DOMAIN ($DOMAIN_IP) no apunta a este servidor ($SERVER_IP)"
    read -p "¿Continuar de todos modos? (s/N): " CONTINUE
    if [ "$CONTINUE" != "s" ] && [ "$CONTINUE" != "S" ]; then
        exit 1
    fi
fi

# -----------------------------------------------------------------------------
# 2. Instalar/configurar Nginx temporalmente sin SSL
# -----------------------------------------------------------------------------
log_info "Configurando Nginx temporal para verificación..."

# Crear directorio para certbot
mkdir -p /var/www/certbot

# Instalar Nginx si no está instalado
if ! command -v nginx &> /dev/null; then
    apt update && apt install -y nginx
fi

# Copiar configuración temporal sin SSL
cp "$NGINX_DIR/nginx-nossl.conf" /etc/nginx/nginx.conf

# Recargar Nginx
nginx -t && systemctl reload nginx

# -----------------------------------------------------------------------------
# 3. Obtener certificado SSL
# -----------------------------------------------------------------------------
log_info "Obteniendo certificado SSL de Let's Encrypt..."

certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

# -----------------------------------------------------------------------------
# 4. Copiar certificados
# -----------------------------------------------------------------------------
log_info "Configurando certificados..."

mkdir -p "$NGINX_DIR/ssl"
cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem "$NGINX_DIR/ssl/"
cp /etc/letsencrypt/live/$DOMAIN/privkey.pem "$NGINX_DIR/ssl/"
chmod 600 "$NGINX_DIR/ssl/"*.pem

# -----------------------------------------------------------------------------
# 5. Actualizar configuración de Nginx con SSL
# -----------------------------------------------------------------------------
log_info "Activando configuración SSL..."

# Actualizar server_name en nginx.conf
sed -i "s/server_name _;/server_name $DOMAIN;/g" "$NGINX_DIR/nginx.conf"

# Copiar configuración final
cp "$NGINX_DIR/nginx.conf" /etc/nginx/nginx.conf

# Verificar y recargar
nginx -t && systemctl reload nginx

# -----------------------------------------------------------------------------
# 6. Configurar renovación automática
# -----------------------------------------------------------------------------
log_info "Configurando renovación automática de certificados..."

# Crear script de renovación
cat > /etc/cron.d/certbot-renew << EOF
0 0 * * * root certbot renew --quiet --post-hook "cp /etc/letsencrypt/live/$DOMAIN/*.pem $NGINX_DIR/ssl/ && nginx -s reload"
EOF

# -----------------------------------------------------------------------------
# 7. Actualizar ALLOWED_ORIGINS en .env
# -----------------------------------------------------------------------------
if [ -f "$PROJECT_DIR/.env" ]; then
    log_info "Actualizando ALLOWED_ORIGINS en .env..."
    sed -i "s|ALLOWED_ORIGINS=.*|ALLOWED_ORIGINS=https://$DOMAIN|g" "$PROJECT_DIR/.env"
fi

# -----------------------------------------------------------------------------
# Resumen
# -----------------------------------------------------------------------------
echo ""
echo "=========================================="
log_info "✅ SSL configurado correctamente!"
echo "=========================================="
echo ""
echo "Tu API ahora está disponible en:"
echo "  https://$DOMAIN"
echo ""
echo "Endpoints:"
echo "  - Health:  https://$DOMAIN/actuator/health"
echo "  - Swagger: https://$DOMAIN/swagger-ui.html"
echo ""
echo "El certificado se renovará automáticamente."
echo ""
