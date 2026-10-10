package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class AnonymousMigrationTest {
  private Flyway flyway(PostgresTestDatabase db, String target) {
    return Flyway.configure().dataSource(db.url(), db.username(), db.password())
        .locations("classpath:db/migration").target(target)
        .baselineOnMigrate(false).cleanDisabled(true).load();
  }

  @Test
  void freshDatabaseSupportsAnonymousOrdersAndPositivePrices() throws Exception {
    try (var db = PostgresTestDatabase.start()) {
      var migration = flyway(db, "2");
      assertEquals(2, migration.migrate().migrationsExecuted);
      migration.validate();
      assertEquals("2", migration.info().current().getVersion().getVersion());
      try (var connection = DriverManager.getConnection(db.url(), db.username(), db.password());
          var statement = connection.createStatement()) {
        statement.executeUpdate("INSERT INTO orders (total_amount, updated_at) VALUES (0, now())");
        try (var result = statement.executeQuery("SELECT customer_id, status FROM orders")) {
          assertTrue(result.next());
          assertNull(result.getObject(1));
          assertNull(result.getObject(2));
        }
        assertThrows(SQLException.class, () -> statement.executeUpdate(
            "INSERT INTO menu_item (name, price) VALUES ('Invalid price', -1)"));
        try (var result = statement.executeQuery("SELECT count(*) FROM push_subscription")) {
          result.next();
          assertEquals(0, result.getInt(1));
        }
      }
    }
  }

  @Test
  void upgradesV1WithoutChangingLegacyRecordsAndBackfillsHistoricalPrice() throws Exception {
    try (var db = PostgresTestDatabase.start()) {
      flyway(db, "1").migrate();
      try (var connection = DriverManager.getConnection(db.url(), db.username(), db.password());
          var statement = connection.createStatement()) {
        statement.executeUpdate("INSERT INTO customer (id, name, phone, email) VALUES (1, 'Legacy customer', '0000000000', 'legacy@example.test')");
        statement.executeUpdate("INSERT INTO notification_preference (id, channel, contact_value) VALUES (1, 'CONSOLE', 'legacy')");
        statement.executeUpdate("INSERT INTO menu_item (id, name, category, price, prep_time_minutes) VALUES (1, 'Legacy menu', 'FOOD', 70, 10)");
        statement.executeUpdate("INSERT INTO orders (id, customer_id, status, total_amount, created_at) VALUES (1, 1, 'WAITING', 100, TIMESTAMP '2024-01-02 03:04:05')");
        statement.executeUpdate("INSERT INTO order_item (id, order_id, menu_item_id, quantity, subtotal) VALUES (1, 1, 1, 2, 100)");
        statement.executeUpdate("INSERT INTO queue (id, queue_number, status, status_changed_at) VALUES (1, 27, 'WAITING', TIMESTAMP '2024-01-02 03:04:05')");
        statement.executeUpdate("INSERT INTO notification_log (id, queue_id, channel, message, success, sent_at) VALUES (1, 1, 'CONSOLE', 'Legacy message', true, TIMESTAMP '2024-01-02 03:04:05')");
        var before = legacyRows(connection);
        var migration = flyway(db, "2");
        assertEquals(1, migration.migrate().migrationsExecuted);
        migration.validate();
        assertEquals(before, legacyRows(connection));
        try (var result = statement.executeQuery("SELECT unit_price, menu_item_name FROM order_item WHERE id = 1")) {
          assertTrue(result.next());
          assertEquals("50.00", result.getBigDecimal(1).toPlainString());
          assertEquals("Legacy menu", result.getString(2));
        }
        try (var result = statement.executeQuery("SELECT updated_at = created_at FROM orders WHERE id = 1")) {
          result.next();
          assertTrue(result.getBoolean(1));
        }
        try (var result = statement.executeQuery("SELECT token_hash FROM queue WHERE id = 1")) {
          result.next();
          assertNull(result.getString(1));
        }
        try (var result = statement.executeQuery("SELECT nextval('queue_number_seq')")) {
          result.next();
          assertEquals(28L, result.getLong(1));
        }
      }
    }
  }

  private Map<String, List<String>> legacyRows(Connection connection) throws SQLException {
    Map<String, String> columns = new LinkedHashMap<>();
    columns.put("customer", "id,name,phone,email,created_at");
    columns.put("notification_preference", "id,channel,contact_value,is_active");
    columns.put("menu_item", "id,name,category,price,prep_time_minutes");
    columns.put("orders", "id,customer_id,status,total_amount,created_at");
    columns.put("order_item", "id,order_id,menu_item_id,quantity,subtotal");
    columns.put("queue", "id,queue_number,status,status_changed_at");
    columns.put("notification_log", "id,queue_id,channel,message,success,sent_at");
    Map<String, List<String>> rows = new LinkedHashMap<>();
    for (var table : columns.entrySet()) {
      try (var statement = connection.createStatement();
          var result = statement.executeQuery("SELECT " + table.getValue() + " FROM " + table.getKey() + " ORDER BY id")) {
        assertTrue(result.next(), table.getKey());
        var values = new java.util.ArrayList<String>();
        for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
          values.add(result.getString(column));
        }
        assertFalse(result.next(), table.getKey());
        rows.put(table.getKey(), values);
      }
    }
    return rows;
  }
}
