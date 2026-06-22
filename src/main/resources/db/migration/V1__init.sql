CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE user_type AS ENUM ('buyer', 'entrepreneur');
CREATE TYPE verification_step AS ENUM ('unverified', 'pending', 'verified');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type user_type NOT NULL DEFAULT 'buyer',
    is_verified BOOLEAN NOT NULL DEFAULT false,
    pastoral_verification BOOLEAN NOT NULL DEFAULT false,
    church VARCHAR(255),
    pastor_name VARCHAR(255),
    verification_step verification_step NOT NULL DEFAULT 'unverified',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE zones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID REFERENCES users(id),
    name VARCHAR(200) NOT NULL,
    owner_name VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    zone VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    slogan VARCHAR(255),
    logo_url VARCHAR(500),
    cover_url VARCHAR(500),
    rating DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    featured BOOLEAN NOT NULL DEFAULT false,
    contact_phone VARCHAR(50) NOT NULL,
    contact_whatsapp VARCHAR(50) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_website VARCHAR(500),
    contact_address TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE business_values (
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    value VARCHAR(50) NOT NULL
);

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    price VARCHAR(50),
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    author_name VARCHAR(100) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT NOT NULL,
    honesty INT NOT NULL CHECK (honesty >= 1 AND honesty <= 5),
    quality INT NOT NULL CHECK (quality >= 1 AND quality <= 5),
    punctuality INT NOT NULL CHECK (punctuality >= 1 AND punctuality <= 5),
    kindness INT NOT NULL CHECK (kindness >= 1 AND kindness <= 5),
    verified_client BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_businesses_category ON businesses(category);
CREATE INDEX idx_businesses_zone ON businesses(zone);
CREATE INDEX idx_businesses_featured ON businesses(featured);
CREATE INDEX idx_businesses_owner ON businesses(owner_id);
CREATE INDEX idx_reviews_business ON reviews(business_id);
CREATE INDEX idx_services_business ON services(business_id);

INSERT INTO categories (name, display_order) VALUES
    ('Servicios Profesionales', 1),
    ('Construcci\u00f3n y Hogar', 2),
    ('Educaci\u00f3n y Mentor\u00eda', 3),
    ('Salud y Bienestar', 4),
    ('Alimentos y Reposter\u00eda', 5),
    ('Tecnolog\u00eda y Dise\u00f1o', 6),
    ('Artesan\u00edas y Confecci\u00f3n', 7);

INSERT INTO zones (name, display_order) VALUES
    ('Zona Centro', 1),
    ('Zona Norte', 2),
    ('Zona Sur', 3),
    ('Zona Este', 4),
    ('Zona Oeste', 5);
