-- ================================================================
-- NearSave Database Schema
-- Run this ONCE before starting the application
-- MySQL 8.0+ required for ST_Distance_Sphere spatial function
-- ================================================================

CREATE DATABASE IF NOT EXISTS nearsave_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nearsave_db;

-- ── USERS TABLE ──────────────────────────────────────────────────
-- Stores RETAILER, CUSTOMER, and ADMIN accounts
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)        NOT NULL,
    email       VARCHAR(150)        NOT NULL UNIQUE,
    password    VARCHAR(255)        NOT NULL,   -- BCrypt hashed
    phone       VARCHAR(15),
    role        ENUM('RETAILER','CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    created_at  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ── SHOPS TABLE ──────────────────────────────────────────────────
-- Each RETAILER user owns one shop
CREATE TABLE IF NOT EXISTS shops (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL UNIQUE,        -- one shop per retailer
    shop_name       VARCHAR(150)    NOT NULL,
    address         TEXT,
    shop_location   POINT           NOT NULL SRID 4326,     -- WGS84 lat/lng
    is_open         BOOLEAN         NOT NULL DEFAULT TRUE,
    is_approved     BOOLEAN         NOT NULL DEFAULT FALSE, -- Admin approval required
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_shop_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    SPATIAL INDEX idx_shop_location (shop_location)
);

-- ── PRODUCTS TABLE ────────────────────────────────────────────────
-- Near-expiry products listed by retailers
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id         BIGINT          NOT NULL,
    shop_name       VARCHAR(150)    NOT NULL,               -- denormalised for display
    shop_location   POINT           NOT NULL SRID 4326,     -- denormalised for ST_ query
    name            VARCHAR(200)    NOT NULL,
    category        ENUM('DAIRY','BAKERY','COSMETICS','SNACKS','BEVERAGES','MEDICINES','OTHER') NOT NULL,
    mrp             DECIMAL(10,2)   NOT NULL,               -- original price
    discounted_price DECIMAL(10,2)  NOT NULL,               -- ~45–50% of MRP
    stock_quantity  INT             NOT NULL DEFAULT 1,
    expiry_date     DATE            NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    CONSTRAINT chk_stock_positive CHECK (stock_quantity >= 0), -- Prevent negative stock
    SPATIAL INDEX idx_product_location (shop_location),
    INDEX idx_product_category (category),
    INDEX idx_product_expiry (expiry_date)
);

-- ── RESERVATIONS TABLE ────────────────────────────────────────────
-- Represents a 30-minute pickup reservation
CREATE TABLE IF NOT EXISTS reservations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    customer_id     BIGINT          NOT NULL,
    token_code      VARCHAR(10)     NOT NULL UNIQUE,        -- 6-character code
    status          ENUM('RESERVED','READY_FOR_PICKUP','COMPLETED','CANCELLED','EXPIRED','NO_SHOW') NOT NULL DEFAULT 'RESERVED',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP       NOT NULL,               -- created_at + 30 min
    completed_at    TIMESTAMP,                              -- set when retailer verifies pickup

    CONSTRAINT fk_reservation_product  FOREIGN KEY (product_id)  REFERENCES products(id),
    CONSTRAINT fk_reservation_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    INDEX idx_reservation_status    (status),
    INDEX idx_reservation_expires   (expires_at),
    INDEX idx_reservation_token     (token_code)
);

-- ── PICKUP EVENTS TABLE ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pickup_events (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id     BIGINT          NOT NULL UNIQUE,
    verified_by        VARCHAR(150)    NOT NULL,
    verified_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_method VARCHAR(50)     NOT NULL, -- TOKEN, QR

    CONSTRAINT fk_pickup_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
);

-- ── NOTIFICATIONS TABLE ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    message     TEXT            NOT NULL,
    type        VARCHAR(50)     NOT NULL,
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── FAVORITES TABLE ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS favorites (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    target_id   BIGINT          NOT NULL,
    target_type ENUM('PRODUCT','SHOP') NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_favorite (user_id, target_id, target_type)
);

-- ── AUDIT LOGS TABLE ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor       VARCHAR(150)    NOT NULL,
    action      VARCHAR(100)    NOT NULL,
    entity_name VARCHAR(100)    NOT NULL,
    entity_id   BIGINT,
    timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata    TEXT
);

-- ── SEED DATA (optional demo data) ───────────────────────────────
-- Password for all users below: "password123" (BCrypt hash)
INSERT IGNORE INTO users (id, name, email, password, phone, role) VALUES
(1, 'Krishna Dairy Owner', 'retailer@demo.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyHFfraO2', '9876543210', 'RETAILER'),
(2, 'Rahul Customer',      'customer@demo.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyHFfraO2', '9123456780', 'CUSTOMER'),
(3, 'NearSave Admin',      'admin@demo.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyHFfraO2', '9999999999', 'ADMIN');

-- Bhopal MP Nagar coordinates: lat=23.2332, lng=77.4272
-- Retailer shop (pre-approved for demo purposes)
INSERT IGNORE INTO shops (id, user_id, shop_name, address, shop_location, is_approved) VALUES
(1, 1, 'Krishna Dairy', 'Plot 12, MP Nagar Zone 1, Bhopal',
 ST_GeomFromText('POINT(23.2332 77.4272)', 4326), TRUE);

INSERT IGNORE INTO products (shop_id, shop_name, shop_location, name, category, mrp, discounted_price, stock_quantity, expiry_date) VALUES
(1, 'Krishna Dairy', ST_GeomFromText('POINT(23.2332 77.4272)', 4326), 'Fresh Paneer 500g',    'DAIRY',     120.00,  66.00, 5, DATE_ADD(CURDATE(), INTERVAL 35 DAY)),
(1, 'Krishna Dairy', ST_GeomFromText('POINT(23.2332 77.4272)', 4326), 'Full Cream Milk 1L',   'DAIRY',      60.00,  33.00, 8, DATE_ADD(CURDATE(), INTERVAL 32 DAY)),
(1, 'Krishna Dairy', ST_GeomFromText('POINT(23.2332 77.4272)', 4326), 'Amul Curd 400g',       'DAIRY',      50.00,  28.00, 4, DATE_ADD(CURDATE(), INTERVAL 33 DAY));
