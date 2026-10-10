# Database baseline

V1 is the preserved prototype migration. It defines seven baseline tables: customer,
notification_preference, menu_item, orders, order_item, queue and notification_log.
Queue uses orders.id as its shared primary key. OrderItem belongs to Order through
order_id and references MenuItem without cascading menu deletion.

This is an intermediate schema. The completed anonymous flow requires V2; menu image
storage requires V4, and daily queue numbering requires V5. V3 seeds sample menus.
Do not start the finished entity mappings against V1 alone or use an existing application
database as a migration test target. New migration evidence must come from a temporary
PostgreSQL database; no new migration test has been run for this restart yet.

The schema owns storage definitions. Queue transitions and access control belong to
Komchan; Push delivery, subscription services and log processing belong to Teetach.
Introducing their tables does not introduce their runtime implementation.
