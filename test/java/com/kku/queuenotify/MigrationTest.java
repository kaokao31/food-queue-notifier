package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;

import com.kku.queuenotify.domain.entity.*;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class MigrationTest {
  @Test
  void v1DataIsPreservedAndPricesBackfilled() throws Exception {
    try (var pg = PostgresTestDatabase.start()) {
      var url = pg.url();
      Flyway.configure().dataSource(url, pg.username(), pg.password()).target("1").load().migrate();
      var columns = new java.util.LinkedHashMap<String, java.util.List<String>>();
      var before = new java.util.LinkedHashMap<String, String>();
      try (var c = DriverManager.getConnection(url, pg.username(), pg.password());
          var s = c.createStatement()) {
        s.execute(
            "insert into customer(name,phone) values('legacy','not-real'); insert into"
                + " menu_item(name,price) values('legacy-menu',50); insert into"
                + " orders(customer_id,status,total_amount,created_at)"
                + " values(1,'PAID',100,'2026-10-08 18:00:00'); insert into"
                + " order_item(order_id,menu_item_id,quantity,subtotal) values(1,1,2,100); insert"
                + " into queue(id,queue_number,status) values(1,9,'COMPLETED');"
                + " insert into notification_preference(id,channel,contact_value,is_active)"
                + " values(1,'LINE','fixture-only',false);"
                + " insert into notification_log(queue_id,channel,message,success,sent_at)"
                + " values(1,'LINE','legacy fixture',true,'2026-10-08 18:10:00');");
        for (var table : java.util.List.of("customer", "notification_preference", "menu_item",
            "orders", "order_item", "queue", "notification_log")) {
          var names = new java.util.ArrayList<String>();
          try (var query = c.prepareStatement("SELECT column_name FROM information_schema.columns"
              + " WHERE table_schema=current_schema() AND table_name=? ORDER BY ordinal_position")) {
            query.setString(1, table);
            try (var rows = query.executeQuery()) {
              while (rows.next()) names.add(rows.getString(1));
            }
          }
          assertFalse(names.isEmpty(), table);
          columns.put(table, names);
          before.put(table, snapshot(c, table, names));
        }
      }
      Flyway.configure().dataSource(url, pg.username(), pg.password()).load().migrate();
      validateHibernate(pg);
      try (var c = DriverManager.getConnection(url, pg.username(), pg.password())) {
        for (var entry : columns.entrySet()) {
          assertEquals(before.get(entry.getKey()), snapshot(c, entry.getKey(), entry.getValue()),
              "Every original V1 value is preserved in " + entry.getKey());
        }
      }
      try (var c = DriverManager.getConnection(url, pg.username(), pg.password());
          var s = c.createStatement();
          var r =
              s.executeQuery(
                  "select unit_price,menu_item_name,(select count(*) from customer) as customers"
                      + " from order_item")) {
        assertTrue(r.next());
        assertEquals("50.00", r.getBigDecimal(1).toString());
        assertEquals("legacy-menu", r.getString(2));
        assertEquals(1, r.getInt(3));
        try (var check = c.createStatement();
            var rows =
                check.executeQuery(
                    "select q.queue_number,q.queue_date,d.last_number from queue q join"
                        + " queue_daily_counter d on d.queue_date=q.queue_date where q.id=1")) {
          assertTrue(rows.next());
          assertEquals(9, rows.getInt(1));
          assertEquals(java.time.LocalDate.parse("2026-10-09"), rows.getDate(2).toLocalDate());
          assertEquals(9, rows.getInt(3));
        }
      }
    }
  }

  private String snapshot(Connection c, String table, java.util.List<String> columns)
      throws SQLException {
    // Identifiers come only from fixed fixture table names and PostgreSQL metadata.
    var selected = columns.stream().map(name -> "\"" + name + "\"")
        .collect(java.util.stream.Collectors.joining(","));
    try (var s = c.createStatement();
        var rows = s.executeQuery("SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY t.id)::text,'[]')"
            + " FROM (SELECT " + selected + " FROM " + table + ") t")) {
      assertTrue(rows.next());
      return rows.getString(1);
    }
  }

  private void validateHibernate(PostgresTestDatabase pg) {
    var configuration = new org.hibernate.cfg.Configuration();
    for (var entity : java.util.List.of(Customer.class, NotificationPreference.class,
        MenuItem.class, MenuImage.class, Order.class, OrderItem.class,
        com.kku.queuenotify.domain.entity.Queue.class, DailyQueueCounter.class,
        PushSubscription.class, NotificationLog.class)) configuration.addAnnotatedClass(entity);
    configuration.setProperty("hibernate.connection.url", pg.url());
    configuration.setProperty("hibernate.connection.username", pg.username());
    configuration.setProperty("hibernate.connection.password", pg.password());
    configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
    configuration.setProperty("hibernate.physical_naming_strategy",
        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
    try (var sessionFactory = configuration.buildSessionFactory()) {
      assertTrue(sessionFactory.isOpen(), "Upgraded V1 database validates against all entities");
    }
  }
}
