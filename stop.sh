#!/bin/bash

# =============================================================================
# Predictify Backend - Stop Script
# =============================================================================
# Este script detiene todos los servicios del proyecto:
# 1. Detiene el proceso del backend Spring Boot (si está corriendo)
# 2. Detiene los contenedores Docker (PostgreSQL y pgAdmin)
# =============================================================================

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

# Detectar Docker Compose
detect_docker_compose() {
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker compose"
    elif command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
    else
        print_error "Docker Compose no está instalado"
        exit 1
    fi
}

# Detener el backend Spring Boot
stop_backend() {
    print_step "Buscando procesos del backend Spring Boot..."
    
    # Buscar procesos de Maven spring-boot:run o Java con el proyecto
    MAVEN_PIDS=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
    JAVA_PIDS=$(pgrep -f "predictifylabs-backend" 2>/dev/null || true)
    
    if [ -n "$MAVEN_PIDS" ]; then
        print_step "Deteniendo proceso Maven (PID: $MAVEN_PIDS)..."
        echo "$MAVEN_PIDS" | xargs -r kill -TERM 2>/dev/null || true
        sleep 2
        # Forzar si aún está corriendo
        echo "$MAVEN_PIDS" | xargs -r kill -9 2>/dev/null || true
        print_success "Proceso Maven detenido"
    fi
    
    if [ -n "$JAVA_PIDS" ]; then
        print_step "Deteniendo proceso Java (PID: $JAVA_PIDS)..."
        echo "$JAVA_PIDS" | xargs -r kill -TERM 2>/dev/null || true
        sleep 2
        # Forzar si aún está corriendo
        echo "$JAVA_PIDS" | xargs -r kill -9 2>/dev/null || true
        print_success "Proceso Java detenido"
    fi
    
    if [ -z "$MAVEN_PIDS" ] && [ -z "$JAVA_PIDS" ]; then
        print_info "No se encontraron procesos del backend corriendo"
    fi
}

# Detener servicios Docker
stop_docker_services() {
    print_step "Deteniendo contenedores Docker..."
    
    # Verificar si hay contenedores corriendo
    if docker ps --format '{{.Names}}' | grep -q "predictify"; then
        $DOCKER_COMPOSE down
        print_success "Contenedores Docker detenidos"
    else
        print_info "No hay contenedores de Predictify corriendo"
    fi
}

# Mostrar estado final
show_final_status() {
    echo ""
    print_info "Estado final:"
    
    # Verificar backend
    if pgrep -f "predictifylabs-backend" > /dev/null 2>&1 || pgrep -f "spring-boot:run" > /dev/null 2>&1; then
        print_warning "Backend: Aún corriendo (puede requerir detención manual)"
    else
        print_success "Backend: Detenido"
    fi
    
    # Verificar Docker
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "predictify"; then
        print_warning "Docker: Contenedores aún corriendo"
    else
        print_success "Docker: Contenedores detenidos"
    fi
}

# Mostrar ayuda
show_help() {
    echo "Uso: ./stop.sh [opción]"
    echo ""
    echo "Opciones:"
    echo "  (sin opción)    Detiene todo (Backend + Docker)"
    echo "  --backend       Solo detiene el backend Spring Boot"
    echo "  --docker        Solo detiene servicios Docker"
    echo "  --force         Detiene todo forzosamente (kill -9)"
    echo "  --clean         Detiene todo y elimina volúmenes Docker"
    echo "  --help          Muestra esta ayuda"
}

# Detener todo forzosamente
force_stop() {
    print_step "Deteniendo forzosamente todos los procesos..."
    
    # Matar todos los procesos relacionados
    pkill -9 -f "spring-boot:run" 2>/dev/null || true
    pkill -9 -f "predictifylabs-backend" 2>/dev/null || true
    
    # Detener Docker
    $DOCKER_COMPOSE down --remove-orphans 2>/dev/null || true
    
    print_success "Todos los procesos detenidos forzosamente"
}

# Limpiar todo incluyendo volúmenes
clean_stop() {
    print_warning "Esto detendrá todo y eliminará los datos de la base de datos. ¿Continuar? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        print_info "Operación cancelada"
        exit 0
    fi
    
    stop_backend
    
    print_step "Deteniendo Docker y eliminando volúmenes..."
    $DOCKER_COMPOSE down -v --remove-orphans
    print_success "Docker detenido y volúmenes eliminados"
}

# Main
main() {
    cd "$(dirname "$0")"
    
    print_header "🛑 PREDICTIFY BACKEND - DETENIENDO SERVICIOS"
    
    detect_docker_compose
    
    case "${1:-}" in
        --help|-h)
            show_help
            ;;
        --backend)
            stop_backend
            ;;
        --docker)
            stop_docker_services
            ;;
        --force)
            force_stop
            ;;
        --clean)
            clean_stop
            ;;
        "")
            stop_backend
            stop_docker_services
            show_final_status
            print_success "Todos los servicios han sido detenidos"
            ;;
        *)
            print_error "Opción desconocida: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
