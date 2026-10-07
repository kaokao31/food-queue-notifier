CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE notification_preference (
    id BIGINT PRIMARY KEY REFERENCES customer(id),
    channel VARCHAR(20) NOT NULL,
    contact_value VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE menu_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price NUMERIC(10,2) NOT NULL,
    prep_time_minutes INT
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);

CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id BIGINT NOT NULL REFERENCES menu_item(id),
    quantity INT NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL
);
CREATE INDEX idx_order_item_order_id ON order_item(order_id);

CREATE TABLE queue (
    id BIGINT PRIMARY KEY REFERENCES orders(id),
    queue_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    status_changed_at TIMESTAMP
);

CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,
    queue_id BIGINT NOT NULL REFERENCES queue(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    message VARCHAR(255) NOT NULL,
    success BOOLEAN NOT NULL,
    sent_at TIMESTAMP
);
CREATE INDEX idx_notification_log_queue_id ON notification_log(queue_id);
