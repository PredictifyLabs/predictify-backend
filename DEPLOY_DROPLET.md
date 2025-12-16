# 🚀 Guía de Despliegue en DigitalOcean Droplet

Guía completa para desplegar Predictify Backend en un **Droplet de DigitalOcean**.

## 📋 Requisitos

- Cuenta en [DigitalOcean](https://cloud.digitalocean.com)
- Dominio configurado (opcional pero recomendado)
- Repositorio Git con el código

## 💰 Costo Estimado

| Recurso | Especificaciones | Costo                 |
| ------- | ---------------- | --------------------- |
| Droplet | 1GB RAM, 1 vCPU  | $6/mes                |
| Droplet | 2GB RAM, 1 vCPU  | $12/mes (recomendado) |

---

## 🚀 Despliegue Rápido (5 minutos)

### Paso 1: Crear Droplet

1. Ve a [cloud.digitalocean.com/droplets](https://cloud.digitalocean.com/droplets)
2. **Create Droplet** con:
   - **Image:** Ubuntu 24.04 LTS
   - **Plan:** Basic → Regular → **$12/mes (2GB RAM)** ← Recomendado
   - **Region:** El más cercano a tus usuarios
   - **Authentication:** SSH Keys

### Paso 2: Conectar al Droplet

```bash
ssh root@TU_IP_DEL_DROPLET
```

### Paso 3: Instalación Automática

```bash
# Descargar e instalar dependencias
curl -sSL https://raw.githubusercontent.com/TU_USUARIO/predictify-backend/main/deploy/install.sh | bash
```

O manualmente:

```bash
apt update && apt install -y docker.io docker-compose-plugin git
systemctl enable docker && systemctl start docker
```

### Paso 4: Clonar y Configurar

```bash
cd /opt
git clone https://github.com/TU_USUARIO/predictify-backend.git
cd predictify-backend

# Configurar variables
cp .env.example .env
nano .env
```

**Configura estos valores en `.env`:**

```env
POSTGRES_PASSWORD=una_contraseña_muy_segura
JWT_SECRET=clave_de_64_caracteres_generada_con_openssl
ALLOWED_ORIGINS=https://tu-frontend.com
```

> 💡 Genera JWT_SECRET con: `openssl rand -hex 32`

### Paso 5: Desplegar

```bash
chmod +x deploy/*.sh
./deploy/deploy.sh --build
```

### Paso 6: Configurar SSL (Recomendado)

```bash
./deploy/setup-ssl.sh api.tu-dominio.com tu-email@ejemplo.com
```

---

## ✅ ¡Listo!

Tu API está disponible en:

- **HTTP:** `http://TU_IP:8081`
- **HTTPS:** `https://api.tu-dominio.com` (si configuraste SSL)

**Endpoints:**

- Health: `/actuator/health`
- Swagger: `/swagger-ui.html`

---

## 📁 Estructura de Scripts

```
deploy/
├── install.sh       # Instalación inicial del Droplet
├── deploy.sh        # Desplegar/actualizar la aplicación
├── backup.sh        # Backup de base de datos
├── restore.sh       # Restaurar backup
├── setup-ssl.sh     # Configurar SSL con Let's Encrypt
└── nginx/
    ├── nginx.conf        # Configuración con SSL
    └── nginx-nossl.conf  # Configuración sin SSL
```

---

## 🔧 Comandos Útiles

```bash
# Ver estado de servicios
docker compose -f docker-compose.prod.yml ps

# Ver logs en tiempo real
docker compose -f docker-compose.prod.yml logs -f api

# Reiniciar API
docker compose -f docker-compose.prod.yml restart api

# Actualizar aplicación
cd /opt/predictify-backend && git pull && ./deploy/deploy.sh --build

# Crear backup
./deploy/backup.sh

# Restaurar backup
./deploy/restore.sh nombre_archivo.sql.gz
```

---

## 🔒 Seguridad Incluida

- ✅ Firewall (UFW) configurado
- ✅ Fail2Ban para protección SSH
- ✅ Usuario non-root en contenedores
- ✅ PostgreSQL no expuesto externamente
- ✅ Rate limiting en Nginx
- ✅ Headers de seguridad HTTP
- ✅ SSL/TLS con Let's Encrypt

---

## ⚡ Optimizaciones para Droplets Pequeños

El proyecto está optimizado para Droplets de 1-2GB RAM:

- **JVM:** Uso máximo de 75% RAM, G1GC
- **PostgreSQL:** Configuración de bajo consumo (64MB shared_buffers)
- **HikariCP:** Pool de 5 conexiones
- **Swap:** 2GB configurado automáticamente
- **Logs:** Rotación automática (máx 30MB)

---

## 🔄 CI/CD con GitHub Actions

Crea `.github/workflows/deploy.yml`:

```yaml
name: Deploy to DigitalOcean

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.DROPLET_IP }}
          username: root
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/predictify-backend
            git pull origin main
            ./deploy/deploy.sh --build
```

**Secrets necesarios en GitHub:**

- `DROPLET_IP`: IP de tu Droplet
- `SSH_PRIVATE_KEY`: Clave SSH privada

---

## 🆘 Solución de Problemas

### La API no inicia

```bash
docker compose -f docker-compose.prod.yml logs api
```

### Error de memoria

Usa un Droplet de 2GB ($12/mes) o ajusta límites en `docker-compose.prod.yml`

### Base de datos no conecta

```bash
docker compose -f docker-compose.prod.yml logs db
docker compose -f docker-compose.prod.yml exec db psql -U postgres -d predictify_db
```

### Renovar certificado SSL manualmente

```bash
certbot renew
cp /etc/letsencrypt/live/tu-dominio.com/*.pem /opt/predictify-backend/deploy/nginx/ssl/
nginx -s reload
```

---

## 📊 Monitoreo

```bash
htop                    # CPU y RAM
docker stats            # Recursos por contenedor
df -h                   # Espacio en disco
```

### Configurar alertas en DigitalOcean

Panel → **Monitoring** → **Create Alert** (CPU >80%, RAM >90%, Disco >85%)

---

## ✅ Checklist Pre-Deploy

- [ ] Variables de entorno configuradas
- [ ] JWT_SECRET es único y seguro (64+ caracteres)
- [ ] ALLOWED_ORIGINS configurado con dominio frontend
- [ ] Base de datos con contraseña segura
- [ ] Dominio configurado (si usas SSL)
- [ ] Backups programados
