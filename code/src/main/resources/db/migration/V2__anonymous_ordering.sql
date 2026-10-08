-- Expand the existing schema without deleting legacy customer records.
CREATE TABLE push_subscription (
 id BIGSERIAL PRIMARY KEY, endpoint TEXT NOT NULL, endpoint_hash VARCHAR(64) NOT NULL UNIQUE,
 p256dh TEXT NOT NULL, auth TEXT NOT NULL, is_active BOOLEAN NOT NULL DEFAULT true,
 created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'), updated_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
ALTER TABLE menu_item ADD COLUMN is_available BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE orders ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN status DROP NOT NULL;
ALTER TABLE orders ADD COLUMN push_subscription_id BIGINT REFERENCES push_subscription(id);
ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP;
UPDATE orders SET updated_at=created_at;
ALTER TABLE orders ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE order_item ADD COLUMN unit_price NUMERIC(10,2);
ALTER TABLE order_item ADD COLUMN menu_item_name VARCHAR(100);
-- A legacy price cannot be reconstructed exactly if division needs rounding.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM order_item WHERE quantity<=0 OR subtotal<0 OR round(subtotal/quantity,2)*quantity<>subtotal) THEN
  RAISE EXCEPTION 'Legacy order item cannot be migrated exactly; inspect data before retrying';
 END IF;
END $$;
UPDATE order_item SET unit_price=subtotal/quantity, menu_item_name=m.name FROM menu_item m WHERE m.id=order_item.menu_item_id;
ALTER TABLE order_item ALTER COLUMN unit_price SET NOT NULL;
ALTER TABLE order_item ALTER COLUMN menu_item_name SET NOT NULL;
ALTER TABLE order_item ADD CONSTRAINT order_quantity_positive CHECK(quantity>0);
ALTER TABLE menu_item ADD CONSTRAINT menu_price_positive CHECK(price>=0);
ALTER TABLE order_item ADD CONSTRAINT order_unit_price_positive CHECK(unit_price>=0);
CREATE UNIQUE INDEX uq_order_menu ON order_item(order_id,menu_item_id);
CREATE INDEX idx_order_menu ON order_item(menu_item_id);
ALTER TABLE queue ADD COLUMN token_hash VARCHAR(64) UNIQUE;
-- Existing queues remain staff-only until a deliberate token issuance process.
CREATE UNIQUE INDEX uq_queue_number ON queue(queue_number);
CREATE SEQUENCE queue_number_seq;
SELECT setval('queue_number_seq',COALESCE((SELECT max(queue_number)::bigint+1 FROM queue),1),false);
CREATE INDEX idx_queue_status_number ON queue(status,queue_number);
CREATE INDEX idx_orders_created ON orders(created_at,id);
CREATE INDEX idx_orders_subscription ON orders(push_subscription_id);
ALTER TABLE notification_log ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'LEGACY';
ALTER TABLE notification_log ADD COLUMN http_status INT;
ALTER TABLE notification_log ADD COLUMN attempted_at TIMESTAMP;
ALTER TABLE notification_log ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'LEGACY';
CREATE UNIQUE INDEX uq_ready_notification ON notification_log(queue_id,event_type) WHERE event_type='READY';
