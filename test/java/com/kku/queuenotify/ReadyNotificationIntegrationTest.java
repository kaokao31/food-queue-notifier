package com.kku.queuenotify;

import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.dto.request.*;
import com.kku.queuenotify.dto.response.OrderResponse;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.service.impl.OrderSubscriptionAttachedEvent;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.springframework.web.context.request.RequestContextHolder;
import com.kku.queuenotify.repository.MenuItemRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real C transactions/access/states and T subscriptions/observer/delivery; only provider is mocked. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties="notification.mode=webpush")
class ReadyNotificationIntegrationTest {
  static final PostgresTestDatabase DATABASE=start();
  static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
  @DynamicPropertySource static void database(DynamicPropertyRegistry r){
    r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);
  }
  @Autowired QueueService queues;@Autowired OrderService orders;@Autowired OrderSubscriptionService subscriptions;
  @Autowired MenuItemRepository menus;@Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager transactions;
  @Autowired ApplicationEventPublisher events;@MockBean NotificationStrategy strategy;
  @BeforeEach void setup(){RequestContextHolder.resetRequestAttributes();reset(strategy);
    jdbc.execute("TRUNCATE TABLE orders,menu_item,queue_daily_counter,push_subscription RESTART IDENTITY CASCADE");}
  @AfterEach void cleanup(){RequestContextHolder.resetRequestAttributes();}
  @AfterAll static void close() throws Exception {DATABASE.close();}
  OrderResponse create(){var menu=menus.saveAndFlush(MenuItem.builder().name("Observer meal").price(new BigDecimal("10.00")).build());
    return orders.create(new OrderRequest(List.of(new OrderRequest.Item(menu.getId(),1))));}
  void attach(OrderResponse order){subscriptions.attach(order.id(),order.queueToken(),new PushSubscriptionRequest(
      "https://fcm.googleapis.com/wp/observer-test",new PushSubscriptionRequest.Keys(PushConfigurationTest.KEY,OrderSubscriptionServiceTest.AUTH)));}
  void ready(OrderResponse order){QueueServiceTest.staff();queues.advance(order.id());queues.advance(order.id());}
  int count(Long id){return jdbc.queryForObject("SELECT count(*) FROM notification_log WHERE queue_id=?",Integer.class,id);}
  void catchup(Long id){new TransactionTemplate(transactions).executeWithoutResult(tx->events.publishEvent(new OrderSubscriptionAttachedEvent(UUID.randomUUID(),id)));}
  @Test void committedReadyDeliversOutsideTransactionAndDuplicateEventDoesNotResend(){
    var order=create();attach(order);
    when(strategy.send(any(),any())).thenAnswer(call->{
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      assertEquals("READY",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
      assertEquals("PENDING",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,order.id()));return 202;
    });ready(order);catchup(order.id());
    assertEquals(1,count(order.id()));assertEquals("ACCEPTED",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,order.id()));
    verify(strategy,times(1)).send(any(),any());
  }
  @Test void rolledBackReadyDoesNotClaimOrSend(){
    var order=create();attach(order);QueueServiceTest.staff();queues.advance(order.id());
    new TransactionTemplate(transactions).executeWithoutResult(tx->{queues.advance(order.id());verifyNoInteractions(strategy);tx.setRollbackOnly();});
    assertEquals("PREPARING",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    assertEquals(0,count(order.id()));verifyNoInteractions(strategy);
  }
  @Test void lateSubscriptionOnReadyCatchesUpOnceAfterAttachmentCommit(){
    var order=create();when(strategy.send(any(),any())).thenReturn(201);ready(order);
    assertEquals(0,count(order.id()));verifyNoInteractions(strategy);
    RequestContextHolder.resetRequestAttributes();attach(order);attach(order);
    assertEquals(1,count(order.id()));verify(strategy,times(1)).send(any(),any());
  }
  @Test void providerFailureKeepsReadyAndRecordsOneFailedAttempt(){
    var order=create();attach(order);when(strategy.send(any(),any())).thenThrow(new IllegalStateException("TEST_SECRET_ENDPOINT"));
    assertDoesNotThrow(()->ready(order));catchup(order.id());
    assertEquals("READY",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    assertEquals("FAILED",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,order.id()));
    assertFalse(jdbc.queryForObject("SELECT message FROM notification_log WHERE queue_id=?",String.class,order.id()).contains("SECRET"));
    assertEquals(1,count(order.id()));verify(strategy,times(1)).send(any(),any());
  }
  @Test void attachmentRollbackAndEndedCatchupCannotSend(){
    var order=create();ready(order);RequestContextHolder.resetRequestAttributes();
    new TransactionTemplate(transactions).executeWithoutResult(tx->{attach(order);tx.setRollbackOnly();});
    assertEquals(0,count(order.id()));verifyNoInteractions(strategy);
    QueueServiceTest.staff();queues.advance(order.id());catchup(order.id());
    assertEquals(0,count(order.id()));verifyNoInteractions(strategy);
  }
}
