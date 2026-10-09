-- Uploaded images are stored separately so menu lists do not load image bytes.
ALTER TABLE menu_item ADD COLUMN image_key VARCHAR(64);
CREATE TABLE menu_item_image (
 menu_item_id BIGINT PRIMARY KEY REFERENCES menu_item(id) ON DELETE CASCADE,
 image_data BYTEA NOT NULL,
 content_type VARCHAR(32) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
 CHECK (octet_length(image_data) BETWEEN 1 AND 2097152)
);
