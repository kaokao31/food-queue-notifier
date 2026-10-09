-- Preserve each existing number, order ID, token, status and notification log.
ALTER TABLE queue ADD COLUMN queue_date DATE;
UPDATE queue q SET queue_date = (o.created_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Bangkok')::date
FROM orders o WHERE o.id=q.id;
ALTER TABLE queue ALTER COLUMN queue_date SET NOT NULL;
DROP INDEX uq_queue_number;
ALTER TABLE queue ADD CONSTRAINT uq_queue_date_number UNIQUE(queue_date,queue_number);
CREATE TABLE queue_daily_counter (
 queue_date DATE PRIMARY KEY,
 last_number INTEGER NOT NULL CHECK(last_number >= 0)
);
INSERT INTO queue_daily_counter(queue_date,last_number)
SELECT queue_date,max(queue_number) FROM queue GROUP BY queue_date;
-- Retain the unused legacy sequence; new orders use the daily counter transaction.
