#!/bin/bash

# =============================================================================
# Predictify Backend - Start Script
# =============================================================================
# Este script levanta todo el proyecto automáticamente:
# 1. Verifica dependencias (Docker, Java, Maven)
# 2. Levanta PostgreSQL y pgAdmin con Docker Compose
# 3. Espera a que la base de datos esté lista
# 4. Compila y ejecuta el backend Spring Boot
# =============================================================================

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
print_header() {
    echo -e "\n${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC} ${BLUE}$1${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}\n"
}

print_step() {
    echo -e "${GREEN}▶${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✖${NC} $1"
}

print_success() {
    echo -e "${GREEN}✔${NC} $1"
}

# Verificar si un comando existe
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 no está instalado. Por favor, instálalo primero."
        exit 1
    fi
    print_success "$1 encontrado"
}

# Verificar dependencias
check_dependencies() {
    print_header "Verificando dependencias"
    
    check_command "docker"
    check_command "java"
    
    # Verificar versión de Java
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        print_error "Se requiere Java 17 o superior. Versión actual: $java_version"
        exit 1
    fi
    print_success "Java versión $java_version"
    
    # Verificar Docker Compose (v2 integrado o standalone)
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker compose"
        print_success "Docker Compose v2 encontrado"
    elif command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
        print_success "Docker Compose encontrado"
    else
        print_error "Docker Compose no está instalado"
        exit 1
    fi
}

# Iniciar servicios de Docker
start_docker_services() {
    print_header "Iniciando servicios Docker"
    
    print_step "Levantando PostgreSQL y pgAdmin..."
    $DOCKER_COMPOSE up -d
    
    print_step "Esperando a que PostgreSQL esté listo..."
    
    # Esperar hasta 60 segundos a que PostgreSQL esté healthy
    max_attempts=30
    attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if docker inspect predictify_db_container --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; then
            print_success "PostgreSQL está listo y saludable"
            break
        fi
        
        attempt=$((attempt + 1))
        echo -ne "\r${YELLOW}⏳${NC} Esperando PostgreSQL... ($attempt/$max_attempts)"
        sleep 2
    done
    
    echo ""
    
    if [ $attempt -eq $max_attempts ]; then
        print_warning "Timeout esperando PostgreSQL. Continuando de todos modos..."
    fi
}

# Compilar el proyecto
compile_project() {
    print_header "Compilando proyecto"
    
    print_step "Ejecutando Maven compile..."
    ./mvnw compile -q
    print_success "Proyecto compilado exitosamente"
}

# Iniciar el backend
start_backend() {
    print_header "Iniciando Backend Spring Boot"
    
    print_info "El backend se iniciará en: http://localhost:8081"
    print_info "Swagger UI: http://localhost:8081/swagger-ui.html"
    print_info "pgAdmin: http://localhost:5050 (admin@predictifylabs.com / secret)"
    print_info "PostgreSQL: localhost:5435 (postgres / secret)"
    echo ""
    print_warning "Presiona Ctrl+C para detener el servidor"
    echo ""
    
    ./mvnw spring-boot:run
}

# Mostrar ayuda
show_help() {
    echo "Uso: ./start.sh [opción]"
    echo ""
    echo "Opciones:"
    echo "  (sin opción)    Inicia todo (Docker + Backend)"
    echo "  --docker        Solo inicia servicios Docker"
    echo "  --backend       Solo inicia el backend (asume Docker ya está corriendo)"
    echo "  --stop          Detiene todos los servicios Docker"
    echo "  --restart       Reinicia todo"
    echo "  --logs          Muestra logs de los contenedores Docker"
    echo "  --status        Muestra estado de los servicios"
    echo "  --clean         Limpia y reinicia todo (borra volúmenes)"
    echo "  --help          Muestra esta ayuda"
}

# Detener servicios
stop_services() {
    print_header "Deteniendo servicios"
    $DOCKER_COMPOSE down
    print_success "Servicios detenidos"
}

# Mostrar logs
show_logs() {
    $DOCKER_COMPOSE logs -f
}

# Mostrar estado
show_status() {
    print_header "Estado de servicios"
    
    echo -e "${BLUE}Docker Containers:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "predictify|NAMES" || echo "No hay contenedores corriendo"
    
    echo ""
    echo -e "${BLUE}Verificando conectividad:${NC}"
    
    # Verificar PostgreSQL
    if docker exec predictify_db_container pg_isready -U postgres -d predictify_db &> /dev/null; then
        print_success "PostgreSQL: Conectado"
    else
        print_error "PostgreSQL: No disponible"
    fi
    
    # Verificar Backend
    if curl -s http://localhost:8081/actuator/health &> /dev/null; then
        print_success "Backend: Corriendo"
    else
        print_warning "Backend: No corriendo"
    fi
}

# Limpiar todo
clean_all() {
    print_header "Limpiando todo"
    
    print_warning "Esto eliminará todos los datos de la base de datos. ¿Continuar? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        print_info "Operación cancelada"
        exit 0
    fi
    
    print_step "Deteniendo contenedores..."
    $DOCKER_COMPOSE down -v
    
    print_step "Limpiando compilación..."
    ./mvnw clean -q
    
    print_success "Limpieza completada"
}

# Main
main() {
    cd "$(dirname "$0")"
    
    case "${1:-}" in
        --help|-h)
            show_help
            ;;
        --docker)
            check_dependencies
            start_docker_services
            print_success "Servicios Docker iniciados"
            ;;
        --backend)
            check_dependencies
            compile_project
            start_backend
            ;;
        --stop)
            DOCKER_COMPOSE="docker compose"
            if ! docker compose version &> /dev/null; then
                DOCKER_COMPOSE="docker-compose"
            fi
            stop_services
            ;;
        --restart)
            DOCKER_COMPOSE="docker compose"
            if ! docker compose version &> /dev/null; then
                DOCKER_COMPOSE="docker-compose"
            fi
            stop_services
            check_dependencies
            start_docker_services
            compile_project
            start_backend
            ;;
        --logs)
            DOCKER_COMPOSE="docker compose"
            if ! docker compose version &> /dev/null; then
                DOCKER_COMPOSE="docker-compose"
            fi
            show_logs
            ;;
        --status)
            DOCKER_COMPOSE="docker compose"
            if ! docker compose version &> /dev/null; then
                DOCKER_COMPOSE="docker-compose"
            fi
            show_status
            ;;
        --clean)
            DOCKER_COMPOSE="docker compose"
            if ! docker compose version &> /dev/null; then
                DOCKER_COMPOSE="docker-compose"
            fi
            clean_all
            ;;
        "")
            print_header "🚀 PREDICTIFY BACKEND - INICIO COMPLETO"
            check_dependencies
            start_docker_services
            compile_project
            start_backend
            ;;
        *)
            print_error "Opción desconocida: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
