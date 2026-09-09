CREATE TABLE app_users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 email VARCHAR(180) NOT NULL UNIQUE,
 password VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL,
 CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN'))
);
CREATE TABLE venues (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 address VARCHAR(200) NOT NULL,
 city VARCHAR(100) NOT NULL,
 capacity INT NOT NULL,
 CONSTRAINT chk_venue_capacity CHECK (capacity > 0)
);
CREATE TABLE categories (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 description VARCHAR(500) NOT NULL
);
CREATE TABLE events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(150) NOT NULL,
 description VARCHAR(4000) NOT NULL,
 starts_at TIMESTAMP NOT NULL,
 venue_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL,
 theme VARCHAR(20) NOT NULL,
 FOREIGN KEY (venue_id) REFERENCES venues(id),
 FOREIGN KEY (category_id) REFERENCES categories(id),
 CONSTRAINT chk_event_status CHECK (status IN ('PUBLISHED', 'CANCELLED'))
);
CREATE TABLE ticket_types (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 event_id BIGINT NOT NULL,
 name VARCHAR(100) NOT NULL,
 price DECIMAL(12,2) NOT NULL,
 quantity INT NOT NULL,
 FOREIGN KEY (event_id) REFERENCES events(id),
 CONSTRAINT chk_type_price CHECK (price >= 0),
 CONSTRAINT chk_type_quantity CHECK (quantity > 0)
);
CREATE TABLE orders (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 event_id BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL,
 total DECIMAL(12,2) NOT NULL,
 status VARCHAR(20) NOT NULL,
 FOREIGN KEY (user_id) REFERENCES app_users(id),
 FOREIGN KEY (event_id) REFERENCES events(id),
 CONSTRAINT chk_order_status CHECK (status IN ('PAID', 'CANCELLED'))
);
CREATE TABLE tickets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT NOT NULL,
 ticket_type_id BIGINT NOT NULL,
 code VARCHAR(36) NOT NULL UNIQUE,
 unit_price DECIMAL(12,2) NOT NULL,
 status VARCHAR(20) NOT NULL,
 FOREIGN KEY (order_id) REFERENCES orders(id),
 FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id),
 CONSTRAINT chk_ticket_status CHECK (status IN ('ACTIVE', 'USED', 'CANCELLED'))
);
CREATE INDEX idx_events_start ON events(starts_at);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_tickets_inventory ON tickets(ticket_type_id, status);
