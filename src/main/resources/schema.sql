-- =============================================================================
-- PREDICTIFY - PostgreSQL Database Schema DDL
-- Versión: 1.0.0
-- Motor: PostgreSQL 15+
-- Descripción: Sistema de gestión de eventos con predicciones de asistencia
-- =============================================================================

-- =============================================================================
-- CONFIGURACIÓN INICIAL Y EXTENSIONES
-- =============================================================================

-- Habilitar extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- Para generación de UUIDs
CREATE EXTENSION IF NOT EXISTS "pgcrypto";       -- Para funciones criptográficas
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- Para búsquedas de texto eficientes
CREATE EXTENSION IF NOT EXISTS "unaccent";       -- Para búsquedas sin acentos

-- =============================================================================
-- TIPOS ENUMERADOS (ENUMS)
-- =============================================================================

-- Roles de usuario
CREATE TYPE user_role AS ENUM ('attendee', 'organizer', 'admin');

-- Estado del evento
CREATE TYPE event_status AS ENUM ('draft', 'published', 'cancelled', 'completed');

-- Categoría del evento
CREATE TYPE event_category AS ENUM (
    'conference', 
    'hackathon', 
    'workshop', 
    'meetup', 
    'networking', 
    'bootcamp', 
    'webinar'
);

-- Tipo de evento
CREATE TYPE event_type AS ENUM ('presencial', 'virtual', 'hibrido');

-- Tipo de ubicación
CREATE TYPE location_type AS ENUM ('physical', 'virtual', 'hybrid');

-- Nivel de predicción
CREATE TYPE prediction_level AS ENUM ('high', 'medium', 'low');

-- Tendencia de predicción
CREATE TYPE prediction_trend AS ENUM ('up', 'down', 'stable');

-- Tipo de factor de predicción
CREATE TYPE factor_type AS ENUM ('positive', 'negative', 'neutral');

-- Impacto del factor
CREATE TYPE factor_impact AS ENUM ('high', 'medium', 'low');

-- =============================================================================
-- TABLAS PRINCIPALES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: users
-- Descripción: Almacena información de usuarios del sistema
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(150) NOT NULL,
    avatar TEXT,
    bio TEXT,
    location VARCHAR(255),
    role user_role NOT NULL DEFAULT 'attendee',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    failed_login_attempts SMALLINT DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_name_length CHECK (LENGTH(TRIM(name)) >= 2)
);

-- Comentarios de la tabla users
COMMENT ON TABLE users IS 'Tabla principal de usuarios del sistema Predictify';
COMMENT ON COLUMN users.password_hash IS 'Hash bcrypt de la contraseña (nunca almacenar en texto plano)';
COMMENT ON COLUMN users.failed_login_attempts IS 'Contador de intentos fallidos para prevención de fuerza bruta';
COMMENT ON COLUMN users.locked_until IS 'Timestamp hasta cuando la cuenta está bloqueada';

-- -----------------------------------------------------------------------------
-- TABLA: user_social_links
-- Descripción: Redes sociales y enlaces externos de usuarios
-- -----------------------------------------------------------------------------
CREATE TABLE user_social_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    twitter VARCHAR(100),
    linkedin VARCHAR(100),
    github VARCHAR(100),
    website TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_social_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT user_social_links_user_unique UNIQUE (user_id),
    CONSTRAINT user_social_website_url CHECK (
        website IS NULL OR website ~* '^https?://'
    )
);

-- -----------------------------------------------------------------------------
-- TABLA: user_preferences
-- Descripción: Preferencias y configuraciones de usuario
-- -----------------------------------------------------------------------------
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    
    -- Notificaciones
    notify_email BOOLEAN NOT NULL DEFAULT TRUE,
    notify_push BOOLEAN NOT NULL DEFAULT TRUE,
    notify_event_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    notify_new_events BOOLEAN NOT NULL DEFAULT TRUE,
    notify_event_updates BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Privacidad
    show_profile BOOLEAN NOT NULL DEFAULT TRUE,
    show_attended_events BOOLEAN NOT NULL DEFAULT FALSE,
    show_saved_events BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT user_preferences_user_unique UNIQUE (user_id)
);

-- -----------------------------------------------------------------------------
-- TABLA: user_interests
-- Descripción: Intereses de los usuarios para recomendaciones
-- -----------------------------------------------------------------------------
CREATE TABLE user_interests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    interest VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_interests_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT user_interests_unique UNIQUE (user_id, interest)
);

-- -----------------------------------------------------------------------------
-- TABLA: user_preferred_categories
-- Descripción: Categorías preferidas por el usuario
-- -----------------------------------------------------------------------------
CREATE TABLE user_preferred_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    category event_category NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_pref_categories_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT user_preferred_categories_unique UNIQUE (user_id, category)
);

-- -----------------------------------------------------------------------------
-- TABLA: user_preferred_locations
-- Descripción: Ubicaciones preferidas por el usuario
-- -----------------------------------------------------------------------------
CREATE TABLE user_preferred_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    location VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_pref_locations_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT user_preferred_locations_unique UNIQUE (user_id, location)
);

-- -----------------------------------------------------------------------------
-- TABLA: organizers
-- Descripción: Perfil de organizador (extensión de usuario)
-- -----------------------------------------------------------------------------
CREATE TABLE organizers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    avatar TEXT,
    bio TEXT,
    email VARCHAR(255),
    website TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    events_count INTEGER NOT NULL DEFAULT 0,
    average_attendance_rate DECIMAL(5, 4) DEFAULT 0.0000,
    total_attendees INTEGER NOT NULL DEFAULT 0,
    rating DECIMAL(3, 2) DEFAULT 0.00,
    rating_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_organizers_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT organizers_user_unique UNIQUE (user_id),
    CONSTRAINT organizers_attendance_rate_range CHECK (
        average_attendance_rate >= 0 AND average_attendance_rate <= 1
    ),
    CONSTRAINT organizers_rating_range CHECK (
        rating >= 0 AND rating <= 5
    )
);

-- -----------------------------------------------------------------------------
-- TABLA: speakers
-- Descripción: Ponentes/Speakers de eventos
-- -----------------------------------------------------------------------------
CREATE TABLE speakers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    title VARCHAR(200),
    company VARCHAR(200),
    avatar TEXT,
    bio TEXT,
    
    -- Redes sociales
    twitter VARCHAR(100),
    linkedin VARCHAR(100),
    github VARCHAR(100),
    website TEXT,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT speakers_name_length CHECK (LENGTH(TRIM(name)) >= 2)
);

-- -----------------------------------------------------------------------------
-- TABLA: tags
-- Descripción: Catálogo de etiquetas para eventos
-- -----------------------------------------------------------------------------
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT tags_name_unique UNIQUE (name),
    CONSTRAINT tags_slug_unique UNIQUE (slug),
    CONSTRAINT tags_slug_format CHECK (slug ~ '^[a-z0-9-]+$')
);

-- -----------------------------------------------------------------------------
-- TABLA: events
-- Descripción: Eventos del sistema
-- -----------------------------------------------------------------------------
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organizer_id UUID NOT NULL,
    
    -- Información básica
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500),
    
    -- Fechas y horarios
    start_date DATE NOT NULL,
    end_date DATE,
    start_time TIME NOT NULL,
    end_time TIME,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    
    -- Clasificación
    category event_category NOT NULL,
    type event_type NOT NULL,
    status event_status NOT NULL DEFAULT 'draft',
    
    -- Multimedia
    image_url TEXT,
    
    -- Capacidad y métricas
    capacity INTEGER NOT NULL,
    interested_count INTEGER NOT NULL DEFAULT 0,
    registered_count INTEGER NOT NULL DEFAULT 0,
    attendees_count INTEGER NOT NULL DEFAULT 0,
    views_count INTEGER NOT NULL DEFAULT 0,
    
    -- Precios
    price DECIMAL(10, 2) DEFAULT 0.00,
    currency CHAR(3) DEFAULT 'USD',
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Flags
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_trending BOOLEAN NOT NULL DEFAULT FALSE,
    is_new BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Auditoría
    published_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) 
        REFERENCES organizers(id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT events_slug_unique UNIQUE (slug),
    CONSTRAINT events_title_length CHECK (LENGTH(TRIM(title)) >= 5),
    CONSTRAINT events_capacity_positive CHECK (capacity > 0),
    CONSTRAINT events_price_non_negative CHECK (price >= 0),
    CONSTRAINT events_dates_valid CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT events_times_valid CHECK (
        end_date IS NOT NULL OR end_time IS NULL OR end_time > start_time
    ),
    CONSTRAINT events_counts_non_negative CHECK (
        interested_count >= 0 AND 
        registered_count >= 0 AND 
        attendees_count >= 0 AND
        views_count >= 0
    ),
    CONSTRAINT events_free_price CHECK (
        (is_free = TRUE AND price = 0) OR 
        (is_free = FALSE AND price > 0)
    )
);

-- Comentarios
COMMENT ON TABLE events IS 'Tabla principal de eventos de Predictify';
COMMENT ON COLUMN events.slug IS 'URL amigable única del evento';
COMMENT ON COLUMN events.interested_count IS 'Contador de usuarios interesados (desnormalizado para performance)';

-- -----------------------------------------------------------------------------
-- TABLA: event_locations
-- Descripción: Ubicación de los eventos
-- -----------------------------------------------------------------------------
CREATE TABLE event_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    type location_type NOT NULL,
    
    -- Ubicación física
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    venue VARCHAR(200),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Ubicación virtual
    virtual_link TEXT,
    virtual_platform VARCHAR(100),
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_locations_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT event_locations_event_unique UNIQUE (event_id),
    CONSTRAINT event_locations_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL AND
         latitude >= -90 AND latitude <= 90 AND
         longitude >= -180 AND longitude <= 180)
    ),
    CONSTRAINT event_locations_physical_required CHECK (
        type = 'virtual' OR 
        (city IS NOT NULL AND country IS NOT NULL)
    ),
    CONSTRAINT event_locations_virtual_required CHECK (
        type = 'physical' OR 
        virtual_link IS NOT NULL
    )
);

-- -----------------------------------------------------------------------------
-- TABLA: event_gallery
-- Descripción: Galería de imágenes de eventos
-- -----------------------------------------------------------------------------
CREATE TABLE event_gallery (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    image_url TEXT NOT NULL,
    alt_text VARCHAR(255),
    sort_order SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_gallery_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- TABLA: event_tags
-- Descripción: Relación muchos-a-muchos entre eventos y tags
-- -----------------------------------------------------------------------------
CREATE TABLE event_tags (
    event_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Primary Key
    PRIMARY KEY (event_id, tag_id),
    
    -- Foreign Keys
    CONSTRAINT fk_event_tags_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_tags_tag FOREIGN KEY (tag_id) 
        REFERENCES tags(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- TABLA: event_speakers
-- Descripción: Relación entre eventos y speakers
-- -----------------------------------------------------------------------------
CREATE TABLE event_speakers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    speaker_id UUID NOT NULL,
    role VARCHAR(100) DEFAULT 'speaker',
    is_keynote BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_speakers_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_speakers_speaker FOREIGN KEY (speaker_id) 
        REFERENCES speakers(id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT event_speakers_unique UNIQUE (event_id, speaker_id)
);

-- -----------------------------------------------------------------------------
-- TABLA: event_agenda
-- Descripción: Agenda/programa de los eventos
-- -----------------------------------------------------------------------------
CREATE TABLE event_agenda (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    speaker_id UUID,
    
    start_time TIME NOT NULL,
    duration_minutes SMALLINT NOT NULL DEFAULT 60,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_agenda_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_agenda_speaker FOREIGN KEY (speaker_id) 
        REFERENCES speakers(id) ON DELETE SET NULL,
    
    -- Constraints
    CONSTRAINT event_agenda_duration_positive CHECK (duration_minutes > 0)
);

-- =============================================================================
-- TABLAS DE INTERACCIÓN USUARIO-EVENTO
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: event_registrations
-- Descripción: Registros de usuarios a eventos
-- -----------------------------------------------------------------------------
CREATE TABLE event_registrations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'registered',
    ticket_code VARCHAR(50),
    attended BOOLEAN DEFAULT FALSE,
    attended_at TIMESTAMPTZ,
    
    -- Información de pago (si aplica)
    amount_paid DECIMAL(10, 2) DEFAULT 0.00,
    payment_status VARCHAR(20) DEFAULT 'pending',
    payment_reference VARCHAR(100),
    
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE RESTRICT,
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT event_registrations_unique UNIQUE (event_id, user_id),
    CONSTRAINT event_registrations_ticket_unique UNIQUE (ticket_code),
    CONSTRAINT event_registrations_status CHECK (
        status IN ('registered', 'confirmed', 'cancelled', 'waitlist')
    ),
    CONSTRAINT event_registrations_payment_status CHECK (
        payment_status IN ('pending', 'completed', 'failed', 'refunded')
    )
);

-- -----------------------------------------------------------------------------
-- TABLA: event_interested
-- Descripción: Usuarios interesados en eventos (sin registro formal)
-- -----------------------------------------------------------------------------
CREATE TABLE event_interested (
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Primary Key
    PRIMARY KEY (event_id, user_id),
    
    -- Foreign Keys
    CONSTRAINT fk_event_interested_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_interested_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- TABLA: saved_events
-- Descripción: Eventos guardados por usuarios
-- -----------------------------------------------------------------------------
CREATE TABLE saved_events (
    user_id UUID NOT NULL,
    event_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Primary Key
    PRIMARY KEY (user_id, event_id),
    
    -- Foreign Keys
    CONSTRAINT fk_saved_events_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_events_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE
);

-- =============================================================================
-- TABLAS DE PREDICCIONES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: prediction_factors_catalog
-- Descripción: Catálogo de factores de predicción
-- -----------------------------------------------------------------------------
CREATE TABLE prediction_factors_catalog (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon VARCHAR(50) NOT NULL,
    type factor_type NOT NULL,
    default_weight DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT prediction_factors_weight_range CHECK (
        default_weight >= -1 AND default_weight <= 1
    )
);

-- Insertar factores predefinidos
INSERT INTO prediction_factors_catalog (id, name, description, icon, type, default_weight) VALUES
    ('high_engagement', 'Engagement alto', 'Alto número de interesados en relación a la capacidad', 'fire', 'positive', 0.15),
    ('trending_topic', 'Tema trending', 'El tema del evento está en tendencia', 'rise', 'positive', 0.12),
    ('verified_organizer', 'Organizador verificado', 'Organizador con historial comprobado', 'safety-certificate', 'positive', 0.10),
    ('good_location', 'Ubicación accesible', 'Ubicación de fácil acceso', 'environment', 'positive', 0.05),
    ('early_registrations', 'Registros anticipados', 'Registros tempranos indican alto interés', 'calendar', 'positive', 0.07),
    ('good_weather', 'Buen clima previsto', 'Condiciones climáticas favorables', 'sun', 'positive', 0.03),
    ('good_organizer_history', 'Buen historial del organizador', 'Alta tasa de asistencia promedio', 'check-circle', 'positive', 0.08),
    ('virtual_event', 'Evento virtual', 'Mayor accesibilidad al ser virtual', 'global', 'positive', 0.05),
    ('small_event', 'Evento pequeño', 'Eventos pequeños suelen tener mayor tasa de asistencia', 'team', 'positive', 0.05),
    ('free_event', 'Evento gratuito', 'Eventos gratis tienen mayor tasa de no-show', 'warning', 'negative', -0.10),
    ('competing_events', 'Eventos competidores', 'Otros eventos similares en la misma fecha', 'disconnect', 'negative', -0.08),
    ('bad_weather', 'Mal clima previsto', 'Condiciones climáticas desfavorables', 'cloud', 'negative', -0.05),
    ('holiday_season', 'Temporada de vacaciones', 'Menor asistencia en época de vacaciones', 'gift', 'negative', -0.06),
    ('weekend_event', 'Evento en fin de semana', 'Puede aumentar o disminuir asistencia según tipo', 'calendar', 'neutral', -0.05);

-- -----------------------------------------------------------------------------
-- TABLA: event_predictions
-- Descripción: Predicciones de asistencia para eventos
-- -----------------------------------------------------------------------------
CREATE TABLE event_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    
    -- Métricas de predicción
    probability SMALLINT NOT NULL,
    level prediction_level NOT NULL,
    confidence SMALLINT NOT NULL,
    
    -- Estimaciones de asistencia
    estimated_min INTEGER NOT NULL,
    estimated_max INTEGER NOT NULL,
    estimated_expected INTEGER NOT NULL,
    
    -- Tendencia
    trend prediction_trend DEFAULT 'stable',
    trend_change DECIMAL(5, 2) DEFAULT 0.00,
    
    -- Timestamps
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_predictions_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT event_predictions_probability_range CHECK (
        probability >= 0 AND probability <= 100
    ),
    CONSTRAINT event_predictions_confidence_range CHECK (
        confidence >= 0 AND confidence <= 100
    ),
    CONSTRAINT event_predictions_estimates_valid CHECK (
        estimated_min >= 0 AND
        estimated_min <= estimated_expected AND
        estimated_expected <= estimated_max
    )
);

-- Índice para obtener la última predicción de un evento
CREATE INDEX idx_event_predictions_latest ON event_predictions (event_id, calculated_at DESC);

-- -----------------------------------------------------------------------------
-- TABLA: event_prediction_factors
-- Descripción: Factores aplicados a una predicción específica
-- -----------------------------------------------------------------------------
CREATE TABLE event_prediction_factors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prediction_id UUID NOT NULL,
    factor_id VARCHAR(50) NOT NULL,
    
    impact factor_impact NOT NULL,
    weight DECIMAL(4, 2) NOT NULL,
    description TEXT,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_prediction_factors_prediction FOREIGN KEY (prediction_id) 
        REFERENCES event_predictions(id) ON DELETE CASCADE,
    CONSTRAINT fk_prediction_factors_factor FOREIGN KEY (factor_id) 
        REFERENCES prediction_factors_catalog(id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT event_prediction_factors_unique UNIQUE (prediction_id, factor_id)
);

-- =============================================================================
-- TABLAS DE ANALYTICS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: event_analytics
-- Descripción: Analíticas agregadas de eventos
-- -----------------------------------------------------------------------------
CREATE TABLE event_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    
    -- Overview
    total_views INTEGER NOT NULL DEFAULT 0,
    total_interested INTEGER NOT NULL DEFAULT 0,
    total_registered INTEGER NOT NULL DEFAULT 0,
    total_attended INTEGER DEFAULT 0,
    conversion_rate DECIMAL(5, 4) DEFAULT 0.0000,
    attendance_rate DECIMAL(5, 4) DEFAULT 0.0000,
    revenue DECIMAL(12, 2) DEFAULT 0.00,
    
    -- Engagement
    email_open_rate DECIMAL(5, 4) DEFAULT 0.0000,
    email_click_rate DECIMAL(5, 4) DEFAULT 0.0000,
    social_shares INTEGER NOT NULL DEFAULT 0,
    average_time_on_page INTEGER DEFAULT 0, -- en segundos
    bounce_rate DECIMAL(5, 4) DEFAULT 0.0000,
    
    -- Precisión de predicción
    predicted_attendance INTEGER,
    actual_attendance INTEGER,
    prediction_accuracy DECIMAL(5, 4),
    
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_analytics_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT event_analytics_rates_valid CHECK (
        conversion_rate >= 0 AND conversion_rate <= 1 AND
        attendance_rate >= 0 AND attendance_rate <= 1 AND
        email_open_rate >= 0 AND email_open_rate <= 1 AND
        email_click_rate >= 0 AND email_click_rate <= 1 AND
        bounce_rate >= 0 AND bounce_rate <= 1
    )
);

-- -----------------------------------------------------------------------------
-- TABLA: registration_trends
-- Descripción: Tendencias de registro día a día
-- -----------------------------------------------------------------------------
CREATE TABLE registration_trends (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    date DATE NOT NULL,
    count INTEGER NOT NULL DEFAULT 0,
    cumulative INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_registration_trends_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT registration_trends_unique UNIQUE (event_id, date),
    CONSTRAINT registration_trends_positive CHECK (count >= 0 AND cumulative >= 0)
);

-- -----------------------------------------------------------------------------
-- TABLA: traffic_sources
-- Descripción: Fuentes de tráfico para eventos
-- -----------------------------------------------------------------------------
CREATE TABLE traffic_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    source VARCHAR(100) NOT NULL,
    visits INTEGER NOT NULL DEFAULT 0,
    percentage DECIMAL(5, 4) DEFAULT 0.0000,
    conversions INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_traffic_sources_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT traffic_sources_unique UNIQUE (event_id, source)
);

-- -----------------------------------------------------------------------------
-- TABLA: event_demographics
-- Descripción: Datos demográficos de asistentes
-- -----------------------------------------------------------------------------
CREATE TABLE event_demographics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    demographic_type VARCHAR(50) NOT NULL, -- 'age_group', 'location', 'industry', 'gender'
    label VARCHAR(100) NOT NULL,
    value INTEGER NOT NULL DEFAULT 0,
    percentage DECIMAL(5, 4) DEFAULT 0.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_event_demographics_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT event_demographics_unique UNIQUE (event_id, demographic_type, label),
    CONSTRAINT event_demographics_type CHECK (
        demographic_type IN ('age_group', 'location', 'industry', 'gender')
    )
);

-- =============================================================================
-- TABLAS DE SESIONES Y SEGURIDAD
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: refresh_tokens
-- Descripción: Tokens de refresco para autenticación JWT
-- -----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info TEXT,
    ip_address INET,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT refresh_tokens_hash_unique UNIQUE (token_hash)
);

-- -----------------------------------------------------------------------------
-- TABLA: audit_log
-- Descripción: Registro de auditoría de acciones importantes
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE SET NULL
);

-- =============================================================================
-- ÍNDICES OPTIMIZADOS
-- =============================================================================

-- Índices para users
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_is_active ON users (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_users_created_at ON users (created_at DESC);

-- Índices para events
CREATE INDEX idx_events_organizer ON events (organizer_id);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_category ON events (category);
CREATE INDEX idx_events_type ON events (type);
CREATE INDEX idx_events_start_date ON events (start_date);
CREATE INDEX idx_events_is_featured ON events (is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_events_is_trending ON events (is_trending) WHERE is_trending = TRUE;
CREATE INDEX idx_events_published ON events (status, published_at) 
    WHERE status = 'published';
CREATE INDEX idx_events_upcoming ON events (start_date, status) 
    WHERE status = 'published' AND start_date >= CURRENT_DATE;

-- Índice para búsqueda full-text en eventos
CREATE INDEX idx_events_search ON events 
    USING GIN (to_tsvector('spanish', title || ' ' || COALESCE(description, '')));

-- Índice trigram para búsqueda aproximada
CREATE INDEX idx_events_title_trgm ON events USING GIN (title gin_trgm_ops);

-- Índices para event_locations
CREATE INDEX idx_event_locations_city ON event_locations (city);
CREATE INDEX idx_event_locations_country ON event_locations (country);
CREATE INDEX idx_event_locations_type ON event_locations (type);
CREATE INDEX idx_event_locations_geo ON event_locations (latitude, longitude) 
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Índices para registrations
CREATE INDEX idx_event_registrations_user ON event_registrations (user_id);
CREATE INDEX idx_event_registrations_event ON event_registrations (event_id);
CREATE INDEX idx_event_registrations_status ON event_registrations (status);
CREATE INDEX idx_event_registrations_attended ON event_registrations (event_id, attended) 
    WHERE attended = TRUE;

-- Índices para predictions
CREATE INDEX idx_event_predictions_event ON event_predictions (event_id);
CREATE INDEX idx_event_predictions_level ON event_predictions (level);

-- Índices para analytics
CREATE INDEX idx_event_analytics_event ON event_analytics (event_id);
CREATE INDEX idx_registration_trends_event_date ON registration_trends (event_id, date);

-- Índices para auditoría
CREATE INDEX idx_audit_log_user ON audit_log (user_id);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON audit_log (created_at DESC);

-- Índices para refresh tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at) 
    WHERE revoked_at IS NULL;

-- =============================================================================
-- FUNCIONES Y TRIGGERS
-- =============================================================================

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a todas las tablas con updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_social_links_updated_at
    BEFORE UPDATE ON user_social_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_organizers_updated_at
    BEFORE UPDATE ON organizers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_speakers_updated_at
    BEFORE UPDATE ON speakers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_locations_updated_at
    BEFORE UPDATE ON event_locations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_agenda_updated_at
    BEFORE UPDATE ON event_agenda
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_registrations_updated_at
    BEFORE UPDATE ON event_registrations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_predictions_updated_at
    BEFORE UPDATE ON event_predictions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_analytics_updated_at
    BEFORE UPDATE ON event_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_traffic_sources_updated_at
    BEFORE UPDATE ON traffic_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_event_demographics_updated_at
    BEFORE UPDATE ON event_demographics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Función para generar slug automáticamente
CREATE OR REPLACE FUNCTION generate_event_slug()
RETURNS TRIGGER AS $$
DECLARE
    base_slug TEXT;
    final_slug TEXT;
    counter INTEGER := 0;
BEGIN
    -- Generar slug base desde el título
    base_slug := LOWER(unaccent(NEW.title));
    base_slug := REGEXP_REPLACE(base_slug, '[^a-z0-9]+', '-', 'g');
    base_slug := TRIM(BOTH '-' FROM base_slug);
    
    final_slug := base_slug;
    
    -- Verificar unicidad y agregar sufijo si es necesario
    WHILE EXISTS (SELECT 1 FROM events WHERE slug = final_slug AND id != COALESCE(NEW.id, uuid_nil())) LOOP
        counter := counter + 1;
        final_slug := base_slug || '-' || counter;
    END LOOP;
    
    NEW.slug := final_slug;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_event_slug_trigger
    BEFORE INSERT OR UPDATE OF title ON events
    FOR EACH ROW
    WHEN (NEW.slug IS NULL OR NEW.slug = '' OR OLD.title IS DISTINCT FROM NEW.title)
    EXECUTE FUNCTION generate_event_slug();

-- Función para actualizar contadores de eventos en organizadores
CREATE OR REPLACE FUNCTION update_organizer_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE organizers 
        SET events_count = events_count + 1,
            updated_at = NOW()
        WHERE id = NEW.organizer_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE organizers 
        SET events_count = GREATEST(0, events_count - 1),
            updated_at = NOW()
        WHERE id = OLD.organizer_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_organizer_stats_trigger
    AFTER INSERT OR DELETE ON events
    FOR EACH ROW EXECUTE FUNCTION update_organizer_stats();

-- Función para actualizar contador de uso de tags
CREATE OR REPLACE FUNCTION update_tag_usage()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE tags SET usage_count = usage_count + 1 WHERE id = NEW.tag_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE tags SET usage_count = GREATEST(0, usage_count - 1) WHERE id = OLD.tag_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_tag_usage_trigger
    AFTER INSERT OR DELETE ON event_tags
    FOR EACH ROW EXECUTE FUNCTION update_tag_usage();

-- Función para actualizar contadores de interesados/registrados en eventos
CREATE OR REPLACE FUNCTION update_event_interest_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE events 
        SET interested_count = interested_count + 1,
            updated_at = NOW()
        WHERE id = NEW.event_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE events 
        SET interested_count = GREATEST(0, interested_count - 1),
            updated_at = NOW()
        WHERE id = OLD.event_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_event_interest_count_trigger
    AFTER INSERT OR DELETE ON event_interested
    FOR EACH ROW EXECUTE FUNCTION update_event_interest_count();

-- Función para actualizar contador de registrados
CREATE OR REPLACE FUNCTION update_event_registration_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE events 
        SET registered_count = registered_count + 1,
            updated_at = NOW()
        WHERE id = NEW.event_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE events 
        SET registered_count = GREATEST(0, registered_count - 1),
            updated_at = NOW()
        WHERE id = OLD.event_id;
    ELSIF TG_OP = 'UPDATE' AND OLD.attended != NEW.attended THEN
        IF NEW.attended = TRUE THEN
            UPDATE events 
            SET attendees_count = attendees_count + 1,
                updated_at = NOW()
            WHERE id = NEW.event_id;
        ELSE
            UPDATE events 
            SET attendees_count = GREATEST(0, attendees_count - 1),
                updated_at = NOW()
            WHERE id = NEW.event_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_event_registration_count_trigger
    AFTER INSERT OR DELETE OR UPDATE OF attended ON event_registrations
    FOR EACH ROW EXECUTE FUNCTION update_event_registration_count();

-- =============================================================================
-- VISTAS ÚTILES
-- =============================================================================

-- Vista de eventos con información completa
CREATE OR REPLACE VIEW v_events_complete AS
SELECT 
    e.*,
    el.type AS location_type,
    el.address,
    el.city,
    el.country,
    el.venue,
    el.virtual_link,
    o.display_name AS organizer_name,
    o.avatar AS organizer_avatar,
    o.is_verified AS organizer_is_verified,
    o.average_attendance_rate AS organizer_attendance_rate,
    ep.probability AS prediction_probability,
    ep.level AS prediction_level,
    ep.confidence AS prediction_confidence,
    ep.estimated_expected AS prediction_estimated
FROM events e
LEFT JOIN event_locations el ON el.event_id = e.id
LEFT JOIN organizers o ON o.id = e.organizer_id
LEFT JOIN LATERAL (
    SELECT * FROM event_predictions 
    WHERE event_id = e.id 
    ORDER BY calculated_at DESC 
    LIMIT 1
) ep ON true;

-- Vista de estadísticas del dashboard
CREATE OR REPLACE VIEW v_dashboard_stats AS
SELECT 
    COUNT(*) FILTER (WHERE status != 'draft') AS total_events,
    COALESCE(SUM(attendees_count), 0) AS total_attendees,
    COALESCE(AVG(
        CASE WHEN registered_count > 0 
        THEN attendees_count::DECIMAL / registered_count 
        ELSE 0 END
    ), 0) AS average_attendance_rate,
    COALESCE(SUM(
        CASE WHEN NOT is_free THEN price * registered_count ELSE 0 END
    ), 0) AS total_revenue,
    COUNT(*) FILTER (WHERE status = 'published' AND start_date >= CURRENT_DATE) AS upcoming_events,
    COUNT(*) FILTER (WHERE status = 'completed') AS completed_events,
    COUNT(*) FILTER (WHERE status = 'draft') AS draft_events
FROM events;

-- =============================================================================
-- POLÍTICAS DE SEGURIDAD (Row Level Security)
-- =============================================================================

-- Habilitar RLS en tablas sensibles
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_social_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE saved_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- =============================================================================
-- TABLAS DE SEGURIDAD ADICIONALES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: email_verifications
-- Descripción: Tokens de verificación de email
-- -----------------------------------------------------------------------------
CREATE TABLE email_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_email_verifications_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT email_verifications_token_unique UNIQUE (token_hash)
);

CREATE INDEX idx_email_verifications_user ON email_verifications (user_id);
CREATE INDEX idx_email_verifications_expires ON email_verifications (expires_at) 
    WHERE verified_at IS NULL;

COMMENT ON TABLE email_verifications IS 'Tokens para verificación de correo electrónico';

-- -----------------------------------------------------------------------------
-- TABLA: password_resets
-- Descripción: Tokens de restablecimiento de contraseña
-- -----------------------------------------------------------------------------
CREATE TABLE password_resets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_password_resets_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT password_resets_token_unique UNIQUE (token_hash)
);

CREATE INDEX idx_password_resets_user ON password_resets (user_id);
CREATE INDEX idx_password_resets_expires ON password_resets (expires_at) 
    WHERE used_at IS NULL;

COMMENT ON TABLE password_resets IS 'Tokens para restablecimiento de contraseña con expiración';

-- -----------------------------------------------------------------------------
-- TABLA: active_sessions
-- Descripción: Sesiones activas de usuarios
-- -----------------------------------------------------------------------------
CREATE TABLE active_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    refresh_token_id UUID,
    device_name VARCHAR(255),
    device_type VARCHAR(50), -- 'desktop', 'mobile', 'tablet'
    browser VARCHAR(100),
    os VARCHAR(100),
    ip_address INET,
    location VARCHAR(255),
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_active_sessions_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_active_sessions_token FOREIGN KEY (refresh_token_id) 
        REFERENCES refresh_tokens(id) ON DELETE CASCADE
);

CREATE INDEX idx_active_sessions_user ON active_sessions (user_id);
CREATE INDEX idx_active_sessions_expires ON active_sessions (expires_at);

COMMENT ON TABLE active_sessions IS 'Gestión de sesiones activas para control de dispositivos';

-- =============================================================================
-- SISTEMA DE PERMISOS Y RUTAS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: permissions
-- Descripción: Catálogo de permisos del sistema
-- -----------------------------------------------------------------------------
CREATE TABLE permissions (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    module VARCHAR(50) NOT NULL, -- 'users', 'events', 'analytics', 'system'
    action VARCHAR(50) NOT NULL, -- 'create', 'read', 'update', 'delete', 'manage'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT permissions_module_action_unique UNIQUE (module, action)
);

COMMENT ON TABLE permissions IS 'Catálogo maestro de permisos del sistema';

-- Insertar permisos del sistema
INSERT INTO permissions (id, name, description, module, action) VALUES
    -- Permisos de Usuarios
    ('users.read', 'Ver usuarios', 'Permite ver listado de usuarios', 'users', 'read'),
    ('users.read.own', 'Ver perfil propio', 'Permite ver el propio perfil', 'users', 'read_own'),
    ('users.update.own', 'Editar perfil propio', 'Permite editar el propio perfil', 'users', 'update_own'),
    ('users.update', 'Editar usuarios', 'Permite editar cualquier usuario', 'users', 'update'),
    ('users.delete', 'Eliminar usuarios', 'Permite eliminar usuarios', 'users', 'delete'),
    ('users.manage', 'Gestionar usuarios', 'Acceso total a gestión de usuarios', 'users', 'manage'),
    ('users.change_role', 'Cambiar roles', 'Permite cambiar roles de usuarios', 'users', 'change_role'),
    
    -- Permisos de Eventos
    ('events.read', 'Ver eventos', 'Permite ver eventos publicados', 'events', 'read'),
    ('events.read.all', 'Ver todos los eventos', 'Permite ver todos los eventos incluyendo borradores', 'events', 'read_all'),
    ('events.create', 'Crear eventos', 'Permite crear nuevos eventos', 'events', 'create'),
    ('events.update.own', 'Editar eventos propios', 'Permite editar eventos propios', 'events', 'update_own'),
    ('events.update', 'Editar cualquier evento', 'Permite editar cualquier evento', 'events', 'update'),
    ('events.delete.own', 'Eliminar eventos propios', 'Permite eliminar eventos propios', 'events', 'delete_own'),
    ('events.delete', 'Eliminar cualquier evento', 'Permite eliminar cualquier evento', 'events', 'delete'),
    ('events.publish', 'Publicar eventos', 'Permite publicar eventos', 'events', 'publish'),
    ('events.feature', 'Destacar eventos', 'Permite marcar eventos como destacados', 'events', 'feature'),
    ('events.manage', 'Gestionar eventos', 'Acceso total a gestión de eventos', 'events', 'manage'),
    
    -- Permisos de Registros/Asistencia
    ('registrations.read.own', 'Ver registros propios', 'Ver eventos donde está registrado', 'registrations', 'read_own'),
    ('registrations.read', 'Ver todos los registros', 'Ver todos los registros de eventos', 'registrations', 'read'),
    ('registrations.create', 'Registrarse a eventos', 'Permite registrarse a eventos', 'registrations', 'create'),
    ('registrations.cancel.own', 'Cancelar registro propio', 'Permite cancelar su propio registro', 'registrations', 'cancel_own'),
    ('registrations.manage', 'Gestionar registros', 'Gestión completa de registros', 'registrations', 'manage'),
    ('registrations.checkin', 'Check-in asistentes', 'Permite hacer check-in de asistentes', 'registrations', 'checkin'),
    
    -- Permisos de Analytics
    ('analytics.read.own', 'Ver analytics propios', 'Ver analytics de eventos propios', 'analytics', 'read_own'),
    ('analytics.read', 'Ver todos los analytics', 'Ver analytics de todos los eventos', 'analytics', 'read'),
    ('analytics.export', 'Exportar reportes', 'Permite exportar reportes y datos', 'analytics', 'export'),
    
    -- Permisos de Predicciones
    ('predictions.read', 'Ver predicciones', 'Permite ver predicciones de eventos', 'predictions', 'read'),
    ('predictions.configure', 'Configurar predicciones', 'Permite configurar parámetros de predicción', 'predictions', 'configure'),
    
    -- Permisos de Organizadores
    ('organizers.read', 'Ver organizadores', 'Ver listado de organizadores', 'organizers', 'read'),
    ('organizers.verify', 'Verificar organizadores', 'Permite verificar organizadores', 'organizers', 'verify'),
    ('organizers.manage', 'Gestionar organizadores', 'Gestión completa de organizadores', 'organizers', 'manage'),
    
    -- Permisos de Sistema
    ('system.settings', 'Configuración del sistema', 'Acceso a configuración general', 'system', 'settings'),
    ('system.audit', 'Ver auditoría', 'Acceso a logs de auditoría', 'system', 'audit'),
    ('system.maintenance', 'Modo mantenimiento', 'Puede activar modo mantenimiento', 'system', 'maintenance'),
    
    -- Permisos de Dashboard
    ('dashboard.admin', 'Dashboard Admin', 'Acceso al dashboard de administrador', 'dashboard', 'admin'),
    ('dashboard.organizer', 'Dashboard Organizador', 'Acceso al dashboard de organizador', 'dashboard', 'organizer'),
    ('dashboard.user', 'Dashboard Usuario', 'Acceso al dashboard de usuario', 'dashboard', 'user');

-- -----------------------------------------------------------------------------
-- TABLA: role_permissions
-- Descripción: Asignación de permisos por rol
-- -----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role user_role NOT NULL,
    permission_id VARCHAR(100) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by UUID,
    
    -- Foreign Keys
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) 
        REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_granted_by FOREIGN KEY (granted_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    
    -- Constraints
    CONSTRAINT role_permissions_unique UNIQUE (role, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions (role);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

COMMENT ON TABLE role_permissions IS 'Mapeo de permisos asignados a cada rol';

-- Asignar permisos al rol ADMIN (acceso total)
INSERT INTO role_permissions (role, permission_id) VALUES
    ('admin', 'users.read'),
    ('admin', 'users.read.own'),
    ('admin', 'users.update.own'),
    ('admin', 'users.update'),
    ('admin', 'users.delete'),
    ('admin', 'users.manage'),
    ('admin', 'users.change_role'),
    ('admin', 'events.read'),
    ('admin', 'events.read.all'),
    ('admin', 'events.create'),
    ('admin', 'events.update.own'),
    ('admin', 'events.update'),
    ('admin', 'events.delete.own'),
    ('admin', 'events.delete'),
    ('admin', 'events.publish'),
    ('admin', 'events.feature'),
    ('admin', 'events.manage'),
    ('admin', 'registrations.read.own'),
    ('admin', 'registrations.read'),
    ('admin', 'registrations.create'),
    ('admin', 'registrations.cancel.own'),
    ('admin', 'registrations.manage'),
    ('admin', 'registrations.checkin'),
    ('admin', 'analytics.read.own'),
    ('admin', 'analytics.read'),
    ('admin', 'analytics.export'),
    ('admin', 'predictions.read'),
    ('admin', 'predictions.configure'),
    ('admin', 'organizers.read'),
    ('admin', 'organizers.verify'),
    ('admin', 'organizers.manage'),
    ('admin', 'system.settings'),
    ('admin', 'system.audit'),
    ('admin', 'system.maintenance'),
    ('admin', 'dashboard.admin'),
    ('admin', 'dashboard.organizer'),
    ('admin', 'dashboard.user');

-- Asignar permisos al rol ORGANIZER
INSERT INTO role_permissions (role, permission_id) VALUES
    ('organizer', 'users.read.own'),
    ('organizer', 'users.update.own'),
    ('organizer', 'events.read'),
    ('organizer', 'events.create'),
    ('organizer', 'events.update.own'),
    ('organizer', 'events.delete.own'),
    ('organizer', 'events.publish'),
    ('organizer', 'registrations.read.own'),
    ('organizer', 'registrations.read'),
    ('organizer', 'registrations.create'),
    ('organizer', 'registrations.cancel.own'),
    ('organizer', 'registrations.checkin'),
    ('organizer', 'analytics.read.own'),
    ('organizer', 'analytics.export'),
    ('organizer', 'predictions.read'),
    ('organizer', 'organizers.read'),
    ('organizer', 'dashboard.organizer'),
    ('organizer', 'dashboard.user');

-- Asignar permisos al rol ATTENDEE (usuario normal)
INSERT INTO role_permissions (role, permission_id) VALUES
    ('attendee', 'users.read.own'),
    ('attendee', 'users.update.own'),
    ('attendee', 'events.read'),
    ('attendee', 'registrations.read.own'),
    ('attendee', 'registrations.create'),
    ('attendee', 'registrations.cancel.own'),
    ('attendee', 'predictions.read'),
    ('attendee', 'organizers.read'),
    ('attendee', 'dashboard.user');

-- -----------------------------------------------------------------------------
-- TABLA: protected_routes
-- Descripción: Rutas protegidas del frontend con permisos requeridos
-- -----------------------------------------------------------------------------
CREATE TABLE protected_routes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    path VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    required_permissions TEXT[] NOT NULL DEFAULT '{}', -- Array de permission_ids
    require_all_permissions BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE = AND, FALSE = OR
    allowed_roles user_role[] NOT NULL DEFAULT '{}',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    redirect_unauthorized VARCHAR(255) DEFAULT '/auth/login',
    redirect_forbidden VARCHAR(255) DEFAULT '/403',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    parent_route_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_protected_routes_parent FOREIGN KEY (parent_route_id) 
        REFERENCES protected_routes(id) ON DELETE SET NULL,
    
    -- Constraints
    CONSTRAINT protected_routes_path_unique UNIQUE (path)
);

CREATE INDEX idx_protected_routes_path ON protected_routes (path);
CREATE INDEX idx_protected_routes_active ON protected_routes (is_active) WHERE is_active = TRUE;

COMMENT ON TABLE protected_routes IS 'Configuración de rutas protegidas para el frontend Angular';

-- Insertar rutas protegidas del sistema
INSERT INTO protected_routes (path, name, description, required_permissions, allowed_roles, is_public) VALUES
    -- Rutas públicas
    ('/', 'Home', 'Página principal', '{}', '{}', TRUE),
    ('/events', 'Eventos', 'Listado de eventos públicos', '{}', '{}', TRUE),
    ('/events/:slug', 'Detalle Evento', 'Detalle de evento público', '{}', '{}', TRUE),
    ('/auth/login', 'Login', 'Inicio de sesión', '{}', '{}', TRUE),
    ('/auth/register', 'Registro', 'Registro de usuario', '{}', '{}', TRUE),
    ('/auth/forgot-password', 'Recuperar Contraseña', 'Solicitud de recuperación', '{}', '{}', TRUE),
    ('/auth/reset-password', 'Restablecer Contraseña', 'Restablecer contraseña', '{}', '{}', TRUE),
    ('/organizers', 'Organizadores', 'Listado de organizadores', '{}', '{}', TRUE),
    ('/organizers/:id', 'Perfil Organizador', 'Perfil público de organizador', '{}', '{}', TRUE),
    
    -- Rutas de usuario autenticado
    ('/profile', 'Mi Perfil', 'Perfil del usuario', ARRAY['users.read.own'], ARRAY['attendee', 'organizer', 'admin']::user_role[], FALSE),
    ('/profile/edit', 'Editar Perfil', 'Edición de perfil', ARRAY['users.update.own'], ARRAY['attendee', 'organizer', 'admin']::user_role[], FALSE),
    ('/profile/settings', 'Configuración', 'Configuración de cuenta', ARRAY['users.update.own'], ARRAY['attendee', 'organizer', 'admin']::user_role[], FALSE),
    ('/my-events', 'Mis Eventos', 'Eventos registrados', ARRAY['registrations.read.own'], ARRAY['attendee', 'organizer', 'admin']::user_role[], FALSE),
    ('/saved-events', 'Eventos Guardados', 'Eventos favoritos', ARRAY['registrations.read.own'], ARRAY['attendee', 'organizer', 'admin']::user_role[], FALSE),
    
    -- Rutas de organizador
    ('/dashboard', 'Dashboard', 'Panel principal organizador', ARRAY['dashboard.organizer'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    ('/dashboard/events', 'Mis Eventos Creados', 'Gestión de eventos propios', ARRAY['events.update.own'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    ('/dashboard/events/new', 'Crear Evento', 'Formulario de nuevo evento', ARRAY['events.create'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    ('/dashboard/events/:id/edit', 'Editar Evento', 'Edición de evento', ARRAY['events.update.own'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    ('/dashboard/events/:id/analytics', 'Analytics Evento', 'Estadísticas del evento', ARRAY['analytics.read.own'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    ('/dashboard/events/:id/attendees', 'Asistentes', 'Gestión de asistentes', ARRAY['registrations.checkin'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    ('/dashboard/analytics', 'Analytics General', 'Estadísticas generales', ARRAY['analytics.read.own'], ARRAY['organizer', 'admin']::user_role[], FALSE),
    
    -- Rutas de administrador
    ('/admin', 'Admin Dashboard', 'Panel de administración', ARRAY['dashboard.admin'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/users', 'Gestión Usuarios', 'Administración de usuarios', ARRAY['users.manage'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/users/:id', 'Detalle Usuario', 'Ver/editar usuario', ARRAY['users.update'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/events', 'Gestión Eventos', 'Administración de eventos', ARRAY['events.manage'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/organizers', 'Gestión Organizadores', 'Verificación de organizadores', ARRAY['organizers.manage'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/analytics', 'Analytics Sistema', 'Estadísticas globales', ARRAY['analytics.read'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/predictions', 'Config Predicciones', 'Configuración de IA', ARRAY['predictions.configure'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/audit', 'Logs Auditoría', 'Registro de actividades', ARRAY['system.audit'], ARRAY['admin']::user_role[], FALSE),
    ('/admin/settings', 'Configuración Sistema', 'Ajustes generales', ARRAY['system.settings'], ARRAY['admin']::user_role[], FALSE);

-- Trigger para updated_at
CREATE TRIGGER update_protected_routes_updated_at
    BEFORE UPDATE ON protected_routes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- POLÍTICAS DE ROW LEVEL SECURITY (RLS) COMPLETAS
-- =============================================================================

-- Función helper para obtener el usuario actual
CREATE OR REPLACE FUNCTION current_user_id() 
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_user_id', TRUE), '')::UUID;
EXCEPTION
    WHEN OTHERS THEN RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función helper para obtener el rol del usuario actual
CREATE OR REPLACE FUNCTION current_user_role() 
RETURNS user_role AS $$
DECLARE
    v_role user_role;
BEGIN
    SELECT role INTO v_role FROM users WHERE id = current_user_id();
    RETURN v_role;
EXCEPTION
    WHEN OTHERS THEN RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para verificar si el usuario tiene un permiso específico
CREATE OR REPLACE FUNCTION has_permission(p_permission_id VARCHAR(100)) 
RETURNS BOOLEAN AS $$
DECLARE
    v_role user_role;
BEGIN
    v_role := current_user_role();
    IF v_role IS NULL THEN
        RETURN FALSE;
    END IF;
    
    RETURN EXISTS (
        SELECT 1 FROM role_permissions 
        WHERE role = v_role AND permission_id = p_permission_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para verificar si el usuario es admin
CREATE OR REPLACE FUNCTION is_admin() 
RETURNS BOOLEAN AS $$
BEGIN
    RETURN current_user_role() = 'admin';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para verificar si el usuario es organizador
CREATE OR REPLACE FUNCTION is_organizer() 
RETURNS BOOLEAN AS $$
BEGIN
    RETURN current_user_role() IN ('admin', 'organizer');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para verificar propiedad de un evento
CREATE OR REPLACE FUNCTION owns_event(p_event_id UUID) 
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM events e
        JOIN organizers o ON o.id = e.organizer_id
        WHERE e.id = p_event_id AND o.user_id = current_user_id()
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- -----------------------------------------------------------------------------
-- POLÍTICAS RLS: users
-- -----------------------------------------------------------------------------

-- Los usuarios pueden ver su propio perfil
CREATE POLICY users_select_own ON users
    FOR SELECT
    USING (id = current_user_id() OR is_admin());

-- Los usuarios pueden actualizar su propio perfil
CREATE POLICY users_update_own ON users
    FOR UPDATE
    USING (id = current_user_id() OR is_admin())
    WITH CHECK (id = current_user_id() OR is_admin());

-- Solo admins pueden insertar usuarios (registro se hace vía función especial)
CREATE POLICY users_insert_admin ON users
    FOR INSERT
    WITH CHECK (is_admin() OR current_user_id() IS NULL);

-- Solo admins pueden eliminar usuarios
CREATE POLICY users_delete_admin ON users
    FOR DELETE
    USING (is_admin());

-- -----------------------------------------------------------------------------
-- POLÍTICAS RLS: user_preferences
-- -----------------------------------------------------------------------------

CREATE POLICY user_preferences_select_own ON user_preferences
    FOR SELECT
    USING (user_id = current_user_id() OR is_admin());

CREATE POLICY user_preferences_update_own ON user_preferences
    FOR UPDATE
    USING (user_id = current_user_id() OR is_admin());

CREATE POLICY user_preferences_insert_own ON user_preferences
    FOR INSERT
    WITH CHECK (user_id = current_user_id() OR is_admin());

CREATE POLICY user_preferences_delete_own ON user_preferences
    FOR DELETE
    USING (user_id = current_user_id() OR is_admin());

-- -----------------------------------------------------------------------------
-- POLÍTICAS RLS: user_social_links
-- -----------------------------------------------------------------------------

CREATE POLICY user_social_links_select_own ON user_social_links
    FOR SELECT
    USING (user_id = current_user_id() OR is_admin());

CREATE POLICY user_social_links_modify_own ON user_social_links
    FOR ALL
    USING (user_id = current_user_id() OR is_admin());

-- -----------------------------------------------------------------------------
-- POLÍTICAS RLS: event_registrations
-- -----------------------------------------------------------------------------

-- Usuarios ven sus propios registros, organizadores ven registros de sus eventos
CREATE POLICY registrations_select ON event_registrations
    FOR SELECT
    USING (
        user_id = current_user_id() 
        OR owns_event(event_id) 
        OR is_admin()
    );

-- Usuarios pueden registrarse a eventos
CREATE POLICY registrations_insert ON event_registrations
    FOR INSERT
    WITH CHECK (user_id = current_user_id());

-- Usuarios pueden cancelar su registro, organizadores pueden gestionar
CREATE POLICY registrations_update ON event_registrations
    FOR UPDATE
    USING (
        user_id = current_user_id() 
        OR owns_event(event_id) 
        OR is_admin()
    );

-- Solo admins pueden eliminar registros directamente
CREATE POLICY registrations_delete ON event_registrations
    FOR DELETE
    USING (is_admin());

-- -----------------------------------------------------------------------------
-- POLÍTICAS RLS: saved_events
-- -----------------------------------------------------------------------------

CREATE POLICY saved_events_select_own ON saved_events
    FOR SELECT
    USING (user_id = current_user_id() OR is_admin());

CREATE POLICY saved_events_modify_own ON saved_events
    FOR ALL
    USING (user_id = current_user_id() OR is_admin());

-- -----------------------------------------------------------------------------
-- POLÍTICAS RLS: refresh_tokens
-- -----------------------------------------------------------------------------

CREATE POLICY refresh_tokens_own ON refresh_tokens
    FOR ALL
    USING (user_id = current_user_id() OR is_admin());

-- =============================================================================
-- ROLES DE BASE DE DATOS Y GRANTS
-- =============================================================================

-- Crear roles de base de datos
DO $$
BEGIN
    -- Rol de aplicación principal
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'predictify_app') THEN
        CREATE ROLE predictify_app WITH LOGIN PASSWORD 'CHANGE_THIS_SECURE_PASSWORD';
    END IF;
    
    -- Rol para usuarios no autenticados (solo lectura pública)
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'predictify_anon') THEN
        CREATE ROLE predictify_anon WITH NOLOGIN;
    END IF;
    
    -- Rol para usuarios autenticados
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'predictify_authenticated') THEN
        CREATE ROLE predictify_authenticated WITH NOLOGIN;
    END IF;
    
    -- Rol para administradores
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'predictify_admin') THEN
        CREATE ROLE predictify_admin WITH NOLOGIN;
    END IF;
    
    -- Rol de solo lectura para reportes
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'predictify_readonly') THEN
        CREATE ROLE predictify_readonly WITH LOGIN PASSWORD 'CHANGE_THIS_READONLY_PASSWORD';
    END IF;
END $$;

-- Jerarquía de roles
GRANT predictify_anon TO predictify_app;
GRANT predictify_authenticated TO predictify_app;
GRANT predictify_admin TO predictify_app;

-- =============================================================================
-- GRANTS PARA ROL ANÓNIMO (predictify_anon)
-- =============================================================================

GRANT USAGE ON SCHEMA public TO predictify_anon;

-- Solo lectura en tablas públicas
GRANT SELECT ON events TO predictify_anon;
GRANT SELECT ON event_locations TO predictify_anon;
GRANT SELECT ON event_gallery TO predictify_anon;
GRANT SELECT ON event_tags TO predictify_anon;
GRANT SELECT ON event_speakers TO predictify_anon;
GRANT SELECT ON event_agenda TO predictify_anon;
GRANT SELECT ON speakers TO predictify_anon;
GRANT SELECT ON tags TO predictify_anon;
GRANT SELECT ON organizers TO predictify_anon;
GRANT SELECT ON event_predictions TO predictify_anon;
GRANT SELECT ON prediction_factors_catalog TO predictify_anon;
GRANT SELECT ON protected_routes TO predictify_anon;

-- Vistas públicas
GRANT SELECT ON v_events_complete TO predictify_anon;
GRANT SELECT ON v_dashboard_stats TO predictify_anon;

-- =============================================================================
-- GRANTS PARA ROL AUTENTICADO (predictify_authenticated)
-- =============================================================================

GRANT USAGE ON SCHEMA public TO predictify_authenticated;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO predictify_authenticated;

-- Permisos en tablas de usuario
GRANT SELECT, UPDATE ON users TO predictify_authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_preferences TO predictify_authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_social_links TO predictify_authenticated;
GRANT SELECT, INSERT, DELETE ON user_interests TO predictify_authenticated;
GRANT SELECT, INSERT, DELETE ON user_preferred_categories TO predictify_authenticated;
GRANT SELECT, INSERT, DELETE ON user_preferred_locations TO predictify_authenticated;

-- Permisos en eventos (lectura)
GRANT SELECT ON events TO predictify_authenticated;
GRANT SELECT ON event_locations TO predictify_authenticated;
GRANT SELECT ON event_gallery TO predictify_authenticated;
GRANT SELECT ON event_tags TO predictify_authenticated;
GRANT SELECT ON event_speakers TO predictify_authenticated;
GRANT SELECT ON event_agenda TO predictify_authenticated;
GRANT SELECT ON speakers TO predictify_authenticated;
GRANT SELECT ON tags TO predictify_authenticated;
GRANT SELECT ON organizers TO predictify_authenticated;

-- Permisos de interacción
GRANT SELECT, INSERT, UPDATE ON event_registrations TO predictify_authenticated;
GRANT SELECT, INSERT, DELETE ON event_interested TO predictify_authenticated;
GRANT SELECT, INSERT, DELETE ON saved_events TO predictify_authenticated;

-- Predicciones y analytics (solo lectura)
GRANT SELECT ON event_predictions TO predictify_authenticated;
GRANT SELECT ON event_prediction_factors TO predictify_authenticated;
GRANT SELECT ON prediction_factors_catalog TO predictify_authenticated;

-- Sesiones y tokens
GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_tokens TO predictify_authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON active_sessions TO predictify_authenticated;

-- Permisos y rutas
GRANT SELECT ON permissions TO predictify_authenticated;
GRANT SELECT ON role_permissions TO predictify_authenticated;
GRANT SELECT ON protected_routes TO predictify_authenticated;

-- Vistas
GRANT SELECT ON v_events_complete TO predictify_authenticated;

-- =============================================================================
-- GRANTS PARA ROL ORGANIZADOR (incluido en authenticated)
-- Nota: Los organizadores usan predictify_authenticated + RLS
-- =============================================================================

-- Organizadores pueden crear/editar sus eventos (controlado por RLS)
-- Las siguientes tablas requieren INSERT/UPDATE para organizadores
GRANT INSERT, UPDATE ON events TO predictify_authenticated;
GRANT INSERT, UPDATE, DELETE ON event_locations TO predictify_authenticated;
GRANT INSERT, UPDATE, DELETE ON event_gallery TO predictify_authenticated;
GRANT INSERT, DELETE ON event_tags TO predictify_authenticated;
GRANT INSERT, UPDATE, DELETE ON event_speakers TO predictify_authenticated;
GRANT INSERT, UPDATE, DELETE ON event_agenda TO predictify_authenticated;
GRANT INSERT, UPDATE ON speakers TO predictify_authenticated;
GRANT INSERT ON tags TO predictify_authenticated;
GRANT INSERT, UPDATE ON organizers TO predictify_authenticated;

-- Analytics de sus eventos
GRANT SELECT ON event_analytics TO predictify_authenticated;
GRANT SELECT ON registration_trends TO predictify_authenticated;
GRANT SELECT ON traffic_sources TO predictify_authenticated;
GRANT SELECT ON event_demographics TO predictify_authenticated;

-- =============================================================================
-- GRANTS PARA ROL ADMIN (predictify_admin)
-- =============================================================================

GRANT USAGE ON SCHEMA public TO predictify_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO predictify_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO predictify_admin;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO predictify_admin;

-- =============================================================================
-- GRANTS PARA ROL SOLO LECTURA (predictify_readonly)
-- =============================================================================

GRANT USAGE ON SCHEMA public TO predictify_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO predictify_readonly;

-- =============================================================================
-- FUNCIONES DE UTILIDAD PARA AUTORIZACIÓN
-- =============================================================================

-- Función para obtener permisos de un usuario
CREATE OR REPLACE FUNCTION get_user_permissions(p_user_id UUID)
RETURNS TABLE(permission_id VARCHAR(100), permission_name VARCHAR(150), module VARCHAR(50), action VARCHAR(50)) AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.name, p.module, p.action
    FROM permissions p
    JOIN role_permissions rp ON rp.permission_id = p.id
    JOIN users u ON u.role = rp.role
    WHERE u.id = p_user_id AND p.is_active = TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para verificar acceso a una ruta
CREATE OR REPLACE FUNCTION can_access_route(p_user_id UUID, p_path VARCHAR(255))
RETURNS BOOLEAN AS $$
DECLARE
    v_route protected_routes;
    v_user_role user_role;
    v_has_permission BOOLEAN;
BEGIN
    -- Obtener la ruta
    SELECT * INTO v_route FROM protected_routes WHERE path = p_path AND is_active = TRUE;
    
    -- Si no existe la ruta, denegar
    IF v_route IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Si es pública, permitir
    IF v_route.is_public THEN
        RETURN TRUE;
    END IF;
    
    -- Si no hay usuario, denegar
    IF p_user_id IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Obtener rol del usuario
    SELECT role INTO v_user_role FROM users WHERE id = p_user_id;
    
    -- Verificar si el rol está permitido
    IF v_user_role = ANY(v_route.allowed_roles) THEN
        -- Verificar permisos requeridos
        IF array_length(v_route.required_permissions, 1) IS NULL OR 
           array_length(v_route.required_permissions, 1) = 0 THEN
            RETURN TRUE;
        END IF;
        
        IF v_route.require_all_permissions THEN
            -- Debe tener TODOS los permisos
            SELECT NOT EXISTS (
                SELECT 1 FROM unnest(v_route.required_permissions) AS req_perm
                WHERE req_perm NOT IN (
                    SELECT rp.permission_id 
                    FROM role_permissions rp 
                    WHERE rp.role = v_user_role
                )
            ) INTO v_has_permission;
        ELSE
            -- Debe tener AL MENOS UN permiso
            SELECT EXISTS (
                SELECT 1 FROM role_permissions rp
                WHERE rp.role = v_user_role 
                AND rp.permission_id = ANY(v_route.required_permissions)
            ) INTO v_has_permission;
        END IF;
        
        RETURN v_has_permission;
    END IF;
    
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para obtener rutas accesibles por un usuario
CREATE OR REPLACE FUNCTION get_accessible_routes(p_user_id UUID)
RETURNS TABLE(
    path VARCHAR(255),
    name VARCHAR(100),
    description TEXT,
    is_public BOOLEAN
) AS $$
DECLARE
    v_user_role user_role;
BEGIN
    IF p_user_id IS NULL THEN
        -- Solo rutas públicas
        RETURN QUERY
        SELECT pr.path, pr.name, pr.description, pr.is_public
        FROM protected_routes pr
        WHERE pr.is_public = TRUE AND pr.is_active = TRUE
        ORDER BY pr.sort_order;
    ELSE
        -- Obtener rol
        SELECT u.role INTO v_user_role FROM users u WHERE u.id = p_user_id;
        
        RETURN QUERY
        SELECT pr.path, pr.name, pr.description, pr.is_public
        FROM protected_routes pr
        WHERE pr.is_active = TRUE
        AND (
            pr.is_public = TRUE
            OR v_user_role = ANY(pr.allowed_roles)
        )
        ORDER BY pr.sort_order;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Función para registrar acción en auditoría
CREATE OR REPLACE FUNCTION log_audit_action(
    p_action VARCHAR(100),
    p_entity_type VARCHAR(50),
    p_entity_id UUID,
    p_old_values JSONB DEFAULT NULL,
    p_new_values JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_audit_id UUID;
BEGIN
    INSERT INTO audit_log (user_id, action, entity_type, entity_id, old_values, new_values)
    VALUES (current_user_id(), p_action, p_entity_type, p_entity_id, p_old_values, p_new_values)
    RETURNING id INTO v_audit_id;
    
    RETURN v_audit_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================================================
-- TRIGGER DE AUDITORÍA AUTOMÁTICA
-- =============================================================================

-- Función genérica de auditoría
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM log_audit_action(
            TG_OP || '_' || TG_TABLE_NAME,
            TG_TABLE_NAME,
            NEW.id,
            NULL,
            to_jsonb(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM log_audit_action(
            TG_OP || '_' || TG_TABLE_NAME,
            TG_TABLE_NAME,
            NEW.id,
            to_jsonb(OLD),
            to_jsonb(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM log_audit_action(
            TG_OP || '_' || TG_TABLE_NAME,
            TG_TABLE_NAME,
            OLD.id,
            to_jsonb(OLD),
            NULL
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Aplicar trigger de auditoría a tablas críticas
CREATE TRIGGER audit_users_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_events_trigger
    AFTER INSERT OR UPDATE OR DELETE ON events
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_organizers_trigger
    AFTER INSERT OR UPDATE OR DELETE ON organizers
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =============================================================================
-- VISTA DE RESUMEN DE PERMISOS POR ROL
-- =============================================================================

CREATE OR REPLACE VIEW v_role_permissions_summary AS
SELECT 
    rp.role,
    p.module,
    array_agg(p.action ORDER BY p.action) AS actions,
    array_agg(p.id ORDER BY p.id) AS permission_ids
FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
GROUP BY rp.role, p.module
ORDER BY rp.role, p.module;

COMMENT ON VIEW v_role_permissions_summary IS 'Resumen de permisos agrupados por rol y módulo';

-- =============================================================================
-- COMENTARIOS FINALES
-- =============================================================================

COMMENT ON SCHEMA public IS 'Schema principal de Predictify - Sistema de predicción de asistencia a eventos';

-- Documentación de roles
COMMENT ON FUNCTION current_user_id() IS 'Retorna el UUID del usuario actual desde app.current_user_id';
COMMENT ON FUNCTION current_user_role() IS 'Retorna el rol del usuario actual';
COMMENT ON FUNCTION has_permission(VARCHAR) IS 'Verifica si el usuario actual tiene un permiso específico';
COMMENT ON FUNCTION is_admin() IS 'Verifica si el usuario actual es administrador';
COMMENT ON FUNCTION is_organizer() IS 'Verifica si el usuario actual es organizador o admin';
COMMENT ON FUNCTION owns_event(UUID) IS 'Verifica si el usuario actual es dueño del evento';
COMMENT ON FUNCTION can_access_route(UUID, VARCHAR) IS 'Verifica si un usuario puede acceder a una ruta específica';
COMMENT ON FUNCTION get_user_permissions(UUID) IS 'Obtiene todos los permisos de un usuario';
COMMENT ON FUNCTION get_accessible_routes(UUID) IS 'Obtiene las rutas accesibles para un usuario';

-- =============================================================================
-- RESUMEN DE ROLES Y PERMISOS
-- =============================================================================
/*
╔══════════════════════════════════════════════════════════════════════════════╗
║                        MATRIZ DE PERMISOS POR ROL                            ║
╠═══════════════════════╦═══════════════╦═══════════════╦══════════════════════╣
║ PERMISO               ║ ADMIN         ║ ORGANIZER     ║ ATTENDEE (Usuario)   ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ USUARIOS              ║               ║               ║                      ║
║ - Ver todos           ║ ✓             ║ ✗             ║ ✗                    ║
║ - Ver propio          ║ ✓             ║ ✓             ║ ✓                    ║
║ - Editar propio       ║ ✓             ║ ✓             ║ ✓                    ║
║ - Editar cualquiera   ║ ✓             ║ ✗             ║ ✗                    ║
║ - Eliminar            ║ ✓             ║ ✗             ║ ✗                    ║
║ - Cambiar roles       ║ ✓             ║ ✗             ║ ✗                    ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ EVENTOS               ║               ║               ║                      ║
║ - Ver publicados      ║ ✓             ║ ✓             ║ ✓                    ║
║ - Ver todos (drafts)  ║ ✓             ║ ✗             ║ ✗                    ║
║ - Crear               ║ ✓             ║ ✓             ║ ✗                    ║
║ - Editar propios      ║ ✓             ║ ✓             ║ ✗                    ║
║ - Editar cualquiera   ║ ✓             ║ ✗             ║ ✗                    ║
║ - Eliminar propios    ║ ✓             ║ ✓             ║ ✗                    ║
║ - Eliminar cualquiera ║ ✓             ║ ✗             ║ ✗                    ║
║ - Publicar            ║ ✓             ║ ✓             ║ ✗                    ║
║ - Destacar            ║ ✓             ║ ✗             ║ ✗                    ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ REGISTROS             ║               ║               ║                      ║
║ - Ver propios         ║ ✓             ║ ✓             ║ ✓                    ║
║ - Ver todos           ║ ✓             ║ ✓ (sus evts)  ║ ✗                    ║
║ - Registrarse         ║ ✓             ║ ✓             ║ ✓                    ║
║ - Cancelar propio     ║ ✓             ║ ✓             ║ ✓                    ║
║ - Check-in            ║ ✓             ║ ✓             ║ ✗                    ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ ANALYTICS             ║               ║               ║                      ║
║ - Ver propios         ║ ✓             ║ ✓             ║ ✗                    ║
║ - Ver todos           ║ ✓             ║ ✗             ║ ✗                    ║
║ - Exportar            ║ ✓             ║ ✓             ║ ✗                    ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ PREDICCIONES          ║               ║               ║                      ║
║ - Ver                 ║ ✓             ║ ✓             ║ ✓                    ║
║ - Configurar          ║ ✓             ║ ✗             ║ ✗                    ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ SISTEMA               ║               ║               ║                      ║
║ - Configuración       ║ ✓             ║ ✗             ║ ✗                    ║
║ - Auditoría           ║ ✓             ║ ✗             ║ ✗                    ║
║ - Verificar orgs      ║ ✓             ║ ✗             ║ ✗                    ║
╠═══════════════════════╬═══════════════╬═══════════════╬══════════════════════╣
║ DASHBOARDS            ║               ║               ║                      ║
║ - Admin               ║ ✓             ║ ✗             ║ ✗                    ║
║ - Organizador         ║ ✓             ║ ✓             ║ ✗                    ║
║ - Usuario             ║ ✓             ║ ✓             ║ ✓                    ║
╚═══════════════════════╩═══════════════╩═══════════════╩══════════════════════╝

NOTAS DE SEGURIDAD:
==================
1. Las contraseñas en los roles de BD deben cambiarse en producción
2. El current_user_id debe setearse en cada request: SET app.current_user_id = 'uuid';
3. Las políticas RLS se activan automáticamente para usuarios no superuser
4. Los tokens JWT deben manejarse en el backend, no en la BD
5. Usar HTTPS siempre en producción
6. Implementar rate limiting en el API Gateway
*/

-- Fin del script DDL
