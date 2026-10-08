package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class DatabaseBaselineTest {
  @Test
  void migratesV1AndValidatesSevenBaselineTables() throws Exception {
    try (var db = PostgresTestDatabase.start()) {
      var flyway = Flyway.configure()
          .dataSource(db.url(), db.username(), db.password())
          .locations("classpath:db/migration")
          .target("1")
          .baselineOnMigrate(false)
          .cleanDisabled(true)
          .load();
      assertEquals(1, flyway.migrate().migrationsExecuted);
      flyway.validate();
      assertEquals("1", flyway.info().current().getVersion().getVersion());
      try (var connection = DriverManager.getConnection(db.url(), db.username(), db.password());
          var statement = connection.createStatement();
          var result = statement.executeQuery("""
              SELECT count(*) FROM information_schema.tables
              WHERE table_schema = current_schema()
                AND table_name IN ('customer', 'notification_preference', 'menu_item',
                                   'orders', 'order_item', 'queue', 'notification_log')
              """)) {
        result.next();
        assertEquals(7, result.getInt(1));
      }
    }
  }
}
