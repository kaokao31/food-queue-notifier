package com.kku.queuenotify;

import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.OrderResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.MenuItemRepository;
import com.kku.queuenotify.service.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL, C access/token/states/service and A order service; staff servlet request is a test fixture. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
@Import(QueueServicePersistenceTest.Events.class)
class QueueServicePersistenceTest {
  static final PostgresTestDatabase DATABASE=start();
  static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
  @DynamicPropertySource static void database(DynamicPropertyRegistry r){
    r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);
  }
  @TestConfiguration(proxyBeanMethods=false) static class Events {
    @Bean QueueServiceTest.Recorder queueRecorder(){return new QueueServiceTest.Recorder();}
  }
  @Autowired QueueService queues;@Autowired OrderService orders;@Autowired MenuItemRepository menus;
  @Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager transactions;@Autowired QueueServiceTest.Recorder recorder;
  @BeforeEach void reset(){RequestContextHolder.resetRequestAttributes();recorder.events.clear();
    jdbc.execute("TRUNCATE TABLE orders,menu_item,queue_daily_counter RESTART IDENTITY CASCADE");}
  @AfterEach void cleanup(){RequestContextHolder.resetRequestAttributes();}
  @AfterAll static void close() throws Exception {DATABASE.close();}
  OrderResponse create(){
    var menu=menus.saveAndFlush(MenuItem.builder().name("Queue fixture meal").price(new BigDecimal("10.00")).build());
    return orders.create(new OrderRequest(List.of(new OrderRequest.Item(menu.getId(),1))));
  }
  @Test void realOwnerReadRejectsWrongTokenAndStaffIsRequiredToAdvance(){
    var order=create();assertEquals(QueueStatus.WAITING,queues.get(order.id(),order.queueToken()).status());
    assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->queues.get(order.id(),"wrong")).getStatus());
    assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->queues.advance(order.id())).getStatus());
    assertEquals("WAITING",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    assertTrue(recorder.events.isEmpty());assertFalse(queues.get(order.id(),order.queueToken()).toString().contains(order.queueToken()));
  }
  @Test void committedLifecyclePersistsStatesAndPublishesAfterCommit(){
    var order=create();QueueServiceTest.staff();
    for(var status:new QueueStatus[]{QueueStatus.PREPARING,QueueStatus.READY,QueueStatus.COMPLETED}){
      assertEquals(status,queues.advance(order.id()).status());
      assertEquals(status.name(),jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    }
    assertEquals(3,recorder.events.size());assertNotNull(queues.get(order.id(),null).statusChangedAt());
    assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->queues.advance(order.id())).getStatus());
    assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->queues.cancel(order.id(),null)).getStatus());
    assertEquals(3,recorder.events.size());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification_log",Integer.class));
  }
  @Test void ownerCancellationPersistsAndWrongTokenLeavesRowUntouched(){
    var order=create();assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->queues.cancel(order.id(),"wrong")).getStatus());
    assertEquals("WAITING",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    assertEquals(QueueStatus.CANCELLED,queues.cancel(order.id(),order.queueToken()).status());
    assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    assertEquals(1,recorder.events.size());
  }
  @Test void callerRollbackRestoresDatabaseStatusTimestampAndSuppressesObserver(){
    var order=create();var before=queues.get(order.id(),order.queueToken());QueueServiceTest.staff();
    new TransactionTemplate(transactions).executeWithoutResult(tx->{
      assertEquals(QueueStatus.PREPARING,queues.advance(order.id()).status());assertTrue(recorder.events.isEmpty());tx.setRollbackOnly();
    });var after=queues.get(order.id(),null);assertEquals(before.status(),after.status());
    assertEquals(before.statusChangedAt(),after.statusChangedAt());assertTrue(recorder.events.isEmpty());
  }
  @Test void editWaitingForTransitionLockRechecksCommittedPreparingState() throws Exception {
    var order=create();var item=order.items().get(0);var entered=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
    final Future<?>[] edit=new Future<?>[1];QueueServiceTest.staff();
    try {
      new TransactionTemplate(transactions).executeWithoutResult(tx->{
        queues.advance(order.id()); // Owns the row lock until this outer transaction commits.
        edit[0]=pool.submit(()->{
          entered.countDown();orders.update(order.id(),order.queueToken(),new OrderRequest(List.of(new OrderRequest.Item(item.menuItemId(),2))));
        });
        try {
          assertTrue(entered.await(5,TimeUnit.SECONDS));
          assertThrows(TimeoutException.class,()->edit[0].get(250,TimeUnit.MILLISECONDS));
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
      });
      var failure=assertThrows(ExecutionException.class,()->edit[0].get(10,TimeUnit.SECONDS));
      assertInstanceOf(ApiException.class,failure.getCause());assertEquals(HttpStatus.CONFLICT,((ApiException)failure.getCause()).getStatus());
      assertEquals(QueueStatus.PREPARING,queues.get(order.id(),null).status());assertEquals(1,orders.get(order.id(),null).items().get(0).quantity());
      assertEquals(new BigDecimal("10.00"),orders.get(order.id(),null).totalAmount());assertEquals(1,recorder.events.size());
    } finally {pool.shutdownNow();}
  }
}
