#!/bin/bash
# =============================================================================
# Predictify Backend - Script de Instalación Inicial para Droplet
# =============================================================================
# Ejecutar como root en un Droplet Ubuntu 22.04/24.04 nuevo
# Uso: curl -sSL https://raw.githubusercontent.com/TU_USUARIO/predictify-backend/main/deploy/install.sh | bash
# =============================================================================

set -e  # Salir si hay error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # Sin color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Verificar que se ejecuta como root
if [ "$EUID" -ne 0 ]; then
    log_error "Este script debe ejecutarse como root"
    exit 1
fi

log_info "=== Iniciando instalación de Predictify Backend ==="

# -----------------------------------------------------------------------------
# 1. Actualizar sistema
# -----------------------------------------------------------------------------
log_info "Actualizando sistema operativo..."
apt update && apt upgrade -y

# -----------------------------------------------------------------------------
# 2. Instalar dependencias básicas
# -----------------------------------------------------------------------------
log_info "Instalando dependencias básicas..."
apt install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    ufw \
    fail2ban \
    htop \
    nano \
    wget \
    unzip

# -----------------------------------------------------------------------------
# 3. Instalar Docker
# -----------------------------------------------------------------------------
log_info "Instalando Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh
    
    # Habilitar Docker al inicio
    systemctl enable docker
    systemctl start docker
else
    log_info "Docker ya está instalado"
fi

# Verificar Docker Compose
log_info "Verificando Docker Compose..."
docker compose version || apt install -y docker-compose-plugin

# -----------------------------------------------------------------------------
# 4. Configurar Firewall
# -----------------------------------------------------------------------------
log_info "Configurando firewall (UFW)..."
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'
ufw --force enable

# -----------------------------------------------------------------------------
# 5. Configurar Fail2Ban para seguridad
# -----------------------------------------------------------------------------
log_info "Configurando Fail2Ban..."
cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
EOF

systemctl enable fail2ban
systemctl restart fail2ban

# -----------------------------------------------------------------------------
# 6. Crear estructura de directorios
# -----------------------------------------------------------------------------
log_info "Creando estructura de directorios..."
mkdir -p /opt/predictify-backend
mkdir -p /opt/predictify-backend/backups
mkdir -p /opt/predictify-backend/logs
mkdir -p /opt/predictify-backend/nginx/ssl

# -----------------------------------------------------------------------------
# 7. Configurar swap (útil para Droplets pequeños)
# -----------------------------------------------------------------------------
log_info "Configurando swap de 2GB..."
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    
    # Optimizar uso de swap
    echo 'vm.swappiness=10' >> /etc/sysctl.conf
    echo 'vm.vfs_cache_pressure=50' >> /etc/sysctl.conf
    sysctl -p
else
    log_info "Swap ya configurado"
fi

# -----------------------------------------------------------------------------
# 8. Configurar límites de sistema
# -----------------------------------------------------------------------------
log_info "Optimizando límites del sistema..."
cat >> /etc/security/limits.conf << 'EOF'
* soft nofile 65535
* hard nofile 65535
EOF

# -----------------------------------------------------------------------------
# 9. Crear usuario para la aplicación (seguridad)
# -----------------------------------------------------------------------------
log_info "Creando usuario 'predictify'..."
if ! id "predictify" &>/dev/null; then
    useradd -m -s /bin/bash predictify
    usermod -aG docker predictify
    chown -R predictify:predictify /opt/predictify-backend
fi

# -----------------------------------------------------------------------------
# 10. Instalar Certbot para SSL
# -----------------------------------------------------------------------------
log_info "Instalando Certbot para SSL..."
apt install -y certbot

# -----------------------------------------------------------------------------
# Resumen
# -----------------------------------------------------------------------------
echo ""
echo "=========================================="
log_info "✅ Instalación completada!"
echo "=========================================="
echo ""
echo "Próximos pasos:"
echo "1. Clonar el repositorio:"
echo "   cd /opt/predictify-backend"
echo "   git clone https://github.com/TU_USUARIO/predictify-backend.git ."
echo ""
echo "2. Configurar variables de entorno:"
echo "   cp .env.example .env"
echo "   nano .env"
echo ""
echo "3. Ejecutar el script de despliegue:"
echo "   ./deploy/deploy.sh"
echo ""
echo "4. (Opcional) Configurar SSL:"
echo "   ./deploy/setup-ssl.sh tu-dominio.com"
echo ""
