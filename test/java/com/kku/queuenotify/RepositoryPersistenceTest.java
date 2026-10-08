package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.domain.entity.*;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.repository.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class RepositoryPersistenceTest {
  static final PostgresTestDatabase DATABASE = startDatabase();
  static PostgresTestDatabase startDatabase() {
    try { return PostgresTestDatabase.start(); }
    catch (Exception e) { throw new IllegalStateException("Cannot start isolated repository test database", e); }
  }
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DATABASE::url);
    registry.add("spring.datasource.username", DATABASE::username);
    registry.add("spring.datasource.password", DATABASE::password);
  }
  @AfterAll static void closeDatabase() throws Exception { DATABASE.close(); }
  @Autowired MenuItemRepository menus;
  @Autowired OrderRepository orders;
  @Autowired OrderItemRepository items;
  @Autowired QueueRepository queues;
  @Autowired PushSubscriptionRepository subscriptions;
  @Autowired NotificationLogRepository logs;
  @Autowired CustomerRepository customers;

  @Test void filtersAvailableMenusAndFindsOrdersByStatusAndMenuHistory() {
    var available = menus.saveAndFlush(MenuItem.builder().name("Available").price(new BigDecimal("25.00")).build());
    menus.saveAndFlush(MenuItem.builder().name("Closed").price(new BigDecimal("30.00")).available(false).build());
    var page = PageRequest.of(0, 8, Sort.by("id"));
    assertEquals(1, menus.findByAvailableTrue(page).getTotalElements());
    assertEquals(available.getId(), menus.lockById(available.getId()).orElseThrow().getId());
    assertFalse(items.existsByMenuItemId(available.getId()));
    var order = new Order(); order.setTotalAmount(new BigDecimal("25.00"));
    order.getOrderItems().add(OrderItem.builder().order(order).menuItem(available).quantity(1)
        .unitPrice(new BigDecimal("25.00")).subtotal(new BigDecimal("25.00")).menuItemName("Available").build());
    order.setQueue(Queue.builder().order(order).queueNumber(1).status(QueueStatus.WAITING).tokenHash("1".repeat(64)).build());
    orders.saveAndFlush(order);
    assertEquals(1, orders.list(QueueStatus.WAITING, page).getTotalElements());
    assertEquals(0, orders.list(QueueStatus.READY, page).getTotalElements());
    assertEquals(1, orders.list(null, page).getTotalElements());
    assertTrue(items.existsByMenuItemId(available.getId()));
    assertEquals(order.getId(), queues.lockById(order.getId()).orElseThrow().getId());
    assertTrue(queues.lockById(Long.MAX_VALUE).isEmpty());
    assertTrue(subscriptions.findByEndpointHash("missing").isEmpty());
    assertFalse(logs.existsByQueueId(order.getId()));
    assertFalse(logs.existsByQueueIdAndEventType(order.getId(), "READY"));
    assertTrue(logs.findByQueueIdOrderByIdDesc(order.getId()).isEmpty());
    assertEquals(0, customers.count());
  }
}
