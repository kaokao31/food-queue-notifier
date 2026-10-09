package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;

import com.kku.queuenotify.domain.entity.*;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

class EntityMappingTest {
  @Test
  void validatesV4MappingsAndPersistsSharedQueueIdAndItemSnapshots() throws Exception {
    try (var db = PostgresTestDatabase.start()) {
      Flyway.configure().dataSource(db.url(), db.username(), db.password())
          .locations("classpath:db/migration").target("4")
          .baselineOnMigrate(false).cleanDisabled(true).load().migrate();
      var configuration = new Configuration()
          .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
          .setProperty("hibernate.connection.url", db.url())
          .setProperty("hibernate.connection.username", db.username())
          .setProperty("hibernate.connection.password", db.password())
          .setProperty("hibernate.hbm2ddl.auto", "validate")
          .setProperty("hibernate.jdbc.time_zone", "UTC");
      for (Class<?> entity : new Class<?>[]{Customer.class, NotificationPreference.class,
          MenuItem.class, MenuImage.class, Order.class, OrderItem.class, Queue.class,
          PushSubscription.class, NotificationLog.class}) {
        configuration.addAnnotatedClass(entity);
      }
      try (var factory = configuration.buildSessionFactory()) {
        Long orderId;
        Long menuId;
        try (var session = factory.openSession()) {
          var transaction = session.beginTransaction();
          var menu = MenuItem.builder().name("Original menu").category("FOOD")
              .price(new BigDecimal("22.50")).prepTimeMinutes(10).build();
          session.persist(menu);
          assertTrue(menu.isAvailable());
          var order = new Order();
          order.setTotalAmount(new BigDecimal("45.00"));
          var item = OrderItem.builder().order(order).menuItem(menu)
              .quantity(2).unitPrice(new BigDecimal("22.50"))
              .menuItemName("Original menu").subtotal(new BigDecimal("45.00")).build();
          order.getOrderItems().add(item);
          var queue = Queue.builder().order(order).queueNumber(1)
              .status(QueueStatus.WAITING).tokenHash("0".repeat(64)).build();
          order.setQueue(queue);
          session.persist(order);
          transaction.commit();
          orderId = order.getId();
          menuId = menu.getId();
          assertEquals(orderId, queue.getId());
          assertNotNull(order.getCreatedAt());
          assertNotNull(order.getUpdatedAt());
        }
        try (var session = factory.openSession()) {
          var transaction = session.beginTransaction();
          var menu = session.find(MenuItem.class, menuId);
          menu.setName("Updated menu");
          menu.setPrice(new BigDecimal("99.00"));
          transaction.commit();
        }
        try (var session = factory.openSession()) {
          var order = session.find(Order.class, orderId);
          assertEquals(orderId, order.getQueue().getId());
          assertEquals(1, order.getOrderItems().size());
          var item = order.getOrderItems().get(0);
          assertEquals("Original menu", item.getMenuItemName());
          assertEquals(new BigDecimal("22.50"), item.getUnitPrice());
          assertEquals(new BigDecimal("45.00"), order.getTotalAmount());
          assertEquals("Updated menu", item.getMenuItem().getName());
          assertEquals(new BigDecimal("99.00"), item.getMenuItem().getPrice());
        }
      }
    }
  }
}
