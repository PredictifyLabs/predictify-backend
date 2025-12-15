# 🚀 Guía de Despliegue en DigitalOcean

Esta guía te llevará paso a paso para desplegar Predictify Backend en DigitalOcean.

## 📋 Opciones de Despliegue

| Opción               | Costo Mensual | Dificultad      | Recomendado para     |
| -------------------- | ------------- | --------------- | -------------------- |
| **App Platform**     | ~$12/mes      | ⭐ Fácil        | Desarrollo, MVP      |
| **Droplet + Docker** | ~$6/mes       | ⭐⭐ Media      | Producción pequeña   |
| **Kubernetes**       | ~$40/mes      | ⭐⭐⭐ Avanzada | Producción escalable |

---

## Opción 1: App Platform (Recomendado para empezar)

### Paso 1: Preparar el Repositorio

1. **Sube tu código a GitHub/GitLab:**
   ```bash
   git add .
   git commit -m "Preparar para despliegue en DigitalOcean"
   git push origin main
   ```

### Paso 2: Crear la App en DigitalOcean

1. Ve a [DigitalOcean App Platform](https://cloud.digitalocean.com/apps)
2. Click en **"Create App"**
3. Conecta tu repositorio de GitHub/GitLab
4. Selecciona el repositorio `predictify-backend`
5. Selecciona la rama `main`

### Paso 3: Configurar el Servicio

1. **Detectará automáticamente el Dockerfile**
2. Configura los recursos:
   - **Instance Size:** Basic ($5/mes) o Basic XXS ($12/mes para más RAM)
   - **Instance Count:** 1

### Paso 4: Agregar Base de Datos

1. Click en **"Add Resource"** → **"Database"**
2. Selecciona **PostgreSQL**
3. **Plan:** Dev Database ($7/mes)
4. **Nombre:** `db`

### Paso 5: Configurar Variables de Entorno

Click en el servicio `api` y agrega estas variables:

| Variable                 | Valor                     |
| ------------------------ | ------------------------- |
| `SPRING_PROFILES_ACTIVE` | `prod`                    |
| `PORT`                   | `8081`                    |
| `DATABASE_HOST`          | `${db.HOSTNAME}`          |
| `DATABASE_PORT`          | `${db.PORT}`              |
| `DATABASE_NAME`          | `${db.DATABASE}`          |
| `DATABASE_USER`          | `${db.USERNAME}`          |
| `DATABASE_PASSWORD`      | `${db.PASSWORD}`          |
| `DATABASE_SSLMODE`       | `require`                 |
| `JWT_SECRET`             | (genera una clave segura) |
| `JWT_EXPIRATION`         | `86400000`                |
| `ALLOWED_ORIGINS`        | `https://tu-frontend.com` |
| `GEMINI_API_KEY`         | (tu clave si usas AI)     |

> 💡 **Generar JWT_SECRET seguro:**
>
> ```bash
> openssl rand -hex 32
> ```

### Paso 6: Configurar Health Check

1. **HTTP Path:** `/actuator/health`
2. **Port:** `8081`
3. **Initial Delay:** `60` segundos
4. **Period:** `30` segundos

### Paso 7: Deploy

1. Click en **"Next"** → **"Create Resources"**
2. Espera 5-10 minutos para el build y deploy
3. Tu API estará disponible en: `https://tu-app-xxxxx.ondigitalocean.app`

---

## Opción 2: Droplet con Docker (Más económico)

### Paso 1: Crear Droplet

1. Ve a [DigitalOcean Droplets](https://cloud.digitalocean.com/droplets)
2. Click **"Create Droplet"**
3. Configura:
   - **Image:** Ubuntu 22.04 LTS
   - **Plan:** Basic - Regular ($6/mes - 1GB RAM, 1 vCPU)
   - **Region:** El más cercano a tus usuarios
   - **Authentication:** SSH Keys (recomendado)

### Paso 2: Conectar al Droplet

```bash
ssh root@TU_IP_DEL_DROPLET
```

### Paso 3: Instalar Docker

```bash
# Actualizar sistema
apt update && apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Instalar Docker Compose
apt install docker-compose-plugin -y

# Verificar instalación
docker --version
docker compose version
```

### Paso 4: Clonar Repositorio

```bash
# Instalar Git
apt install git -y

# Clonar proyecto
cd /opt
git clone https://github.com/TU_USUARIO/predictify-backend.git
cd predictify-backend
```

### Paso 5: Configurar Variables de Entorno

```bash
# Crear archivo .env
cp .env.example .env

# Editar con tus valores
nano .env
```

Configura los valores:

```env
POSTGRES_PASSWORD=una_contraseña_muy_segura_aqui
JWT_SECRET=tu_clave_jwt_de_64_caracteres
ALLOWED_ORIGINS=https://tu-frontend.com
```

### Paso 6: Iniciar Servicios

```bash
# Construir e iniciar
docker compose -f docker-compose.prod.yml up -d --build

# Ver logs
docker compose -f docker-compose.prod.yml logs -f api

# Verificar estado
docker compose -f docker-compose.prod.yml ps
```

### Paso 7: Configurar Firewall

```bash
# Habilitar UFW
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 8081/tcp  # API (temporalmente, luego usar Nginx)
ufw enable
```

### Paso 8: Configurar Dominio (Opcional pero recomendado)

1. En **DigitalOcean** → **Networking** → **Domains**
2. Agrega tu dominio
3. Crea un registro **A** apuntando a la IP del Droplet

---

## 🔧 Configuración de SSL/HTTPS

### Con Nginx y Let's Encrypt

```bash
# Instalar Certbot
apt install certbot -y

# Obtener certificado
certbot certonly --standalone -d api.tu-dominio.com

# Los certificados estarán en:
# /etc/letsencrypt/live/api.tu-dominio.com/
```

---

## 🔄 Actualizaciones y CI/CD

### Actualizar Manualmente (Droplet)

```bash
cd /opt/predictify-backend
git pull origin main
docker compose -f docker-compose.prod.yml up -d --build
```

### Configurar GitHub Actions (Recomendado)

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
      - name: Deploy to Droplet
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.DROPLET_IP }}
          username: root
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/predictify-backend
            git pull origin main
            docker compose -f docker-compose.prod.yml up -d --build
```

---

## 📊 Monitoreo

### Verificar Estado de la API

```bash
# Health check
curl https://tu-api.com/actuator/health

# Ver logs
docker compose -f docker-compose.prod.yml logs -f api
```

### Configurar Alertas en DigitalOcean

1. Ve a **Monitoring** en el panel de DigitalOcean
2. Configura alertas para CPU, RAM y disco

---

## 🆘 Solución de Problemas

### La API no inicia

```bash
# Ver logs detallados
docker compose -f docker-compose.prod.yml logs api

# Verificar que la DB esté lista
docker compose -f docker-compose.prod.yml logs db
```

### Error de conexión a base de datos

```bash
# Verificar conectividad
docker compose -f docker-compose.prod.yml exec api ping db

# Verificar variables
docker compose -f docker-compose.prod.yml exec api env | grep DATABASE
```

### Reiniciar servicios

```bash
docker compose -f docker-compose.prod.yml restart api
```

---

## 💰 Resumen de Costos

### App Platform (Fácil)

- Servicio API: $5-12/mes
- Base de datos: $7-15/mes
- **Total: ~$12-27/mes**

### Droplet (Económico)

- Droplet 1GB: $6/mes
- (DB incluida en Docker)
- **Total: ~$6/mes**

### Droplet + Managed Database (Producción)

- Droplet 2GB: $12/mes
- Managed PostgreSQL: $15/mes
- **Total: ~$27/mes**

---

## ✅ Checklist Pre-Deploy

- [ ] Variables de entorno configuradas
- [ ] JWT_SECRET es único y seguro (64+ caracteres)
- [ ] ALLOWED_ORIGINS configurado con tu dominio frontend
- [ ] Base de datos con contraseña segura
- [ ] Dominio configurado (opcional)
- [ ] SSL/HTTPS configurado
- [ ] Backups de base de datos habilitados
- [ ] Monitoreo configurado
