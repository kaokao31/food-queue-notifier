package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.OrderService;
import com.kku.queuenotify.support.OrderingTestDoubles;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
@Import(OrderingTestDoubles.class)
class OrderCrudPersistenceTest {
 static final PostgresTestDatabase DATABASE=start();static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);}
 @AfterAll static void close() throws Exception {DATABASE.close();}
 @Autowired OrderService service;@Autowired MenuItemRepository menus;@Autowired OrderRepository orders;@Autowired QueueRepository queues;@Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;@Autowired OrderingTestDoubles.Access access;@Autowired OrderingTestDoubles.Tokens tokens;
 @BeforeEach void resetIsolatedDatabaseFixtures(){access.staff=false;tokens.failHash=false;jdbc.execute("TRUNCATE TABLE orders,menu_item,queue_daily_counter RESTART IDENTITY CASCADE");}
 MenuItem menu(String name,String price){return menus.saveAndFlush(MenuItem.builder().name(name).price(new BigDecimal(price)).build());}
 OrderRequest request(Long id,int quantity){return new OrderRequest(List.of(new OrderRequest.Item(id,quantity)));}
 @Test void createsSnapshotAndDailyNumberAndOnlyReturnsTokenAtCreation(){
  var menu=menu("Original","12.50");var first=service.create(request(menu.getId(),2));assertEquals(new BigDecimal("25.00"),first.totalAmount());assertEquals(1,first.queue().queueNumber());assertNotNull(first.queue().queueDate());assertNotNull(first.queueToken());
  menu.setName("Changed");menu.setPrice(new BigDecimal("99.00"));menus.saveAndFlush(menu);var read=service.get(first.id(),first.queueToken());assertNull(read.queueToken());assertEquals("Original",read.items().get(0).menuItemName());assertEquals(new BigDecimal("12.50"),read.items().get(0).unitPrice());
  assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->service.get(first.id(),"wrong")).getStatus());var second=service.create(request(menu.getId(),1));assertEquals(2,second.queue().queueNumber());assertNotEquals(first.queueToken(),second.queueToken());
  assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->service.list(null,PageRequest.of(0,20))).getStatus());access.staff=true;assertEquals(2,service.list(QueueStatus.WAITING,PageRequest.of(0,20)).totalElements());
 }
 @Test void replacesItemsOnlyWhileWaitingAndRollsBackInvalidEdits(){
  var menu=menu("Rice","10.00");var created=service.create(request(menu.getId(),1));var changed=service.update(created.id(),created.queueToken(),request(menu.getId(),3));assertEquals(new BigDecimal("30.00"),changed.totalAmount());assertEquals(1,changed.items().size());assertNull(changed.queueToken());
  var duplicate=new OrderRequest(List.of(new OrderRequest.Item(menu.getId(),1),new OrderRequest.Item(menu.getId(),2)));assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException.class,()->service.update(created.id(),created.queueToken(),duplicate)).getStatus());assertEquals(new BigDecimal("30.00"),service.get(created.id(),created.queueToken()).totalAmount());
  new TransactionTemplate(manager).executeWithoutResult(s->{var q=queues.lockById(created.id()).orElseThrow();q.setStatus(QueueStatus.PREPARING);queues.saveAndFlush(q);});assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.update(created.id(),created.queueToken(),request(menu.getId(),1))).getStatus());
 }
 @Test void staffDeleteProtectsLogsAndDoesNotReuseDailyNumbers(){
  var menu=menu("Rice","10.00");var first=service.create(request(menu.getId(),1));assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->service.delete(first.id())).getStatus());access.staff=true;service.delete(first.id());assertFalse(orders.existsById(first.id()));assertFalse(queues.existsById(first.id()));var second=service.create(request(menu.getId(),1));assertEquals(2,second.queue().queueNumber());
  jdbc.update("INSERT INTO notification_log(queue_id,channel,message,success) VALUES (?,'CONSOLE','history',true)",second.id());assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.delete(second.id())).getStatus());assertTrue(orders.existsById(second.id()));
 }
 @Test void closedMenuAndProviderFailureLeaveNoOrdersOrAllocatedNumbers(){
  var menu=menu("Rice","10.00");menu.setAvailable(false);menus.saveAndFlush(menu);assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.create(request(menu.getId(),1))).getStatus());assertEquals(0,orders.count());
  menu.setAvailable(true);menus.saveAndFlush(menu);tokens.failHash=true;assertThrows(IllegalStateException.class,()->service.create(request(menu.getId(),1)));tokens.failHash=false;assertEquals(0,orders.count());assertEquals(0,queues.count());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM queue_daily_counter",Integer.class));assertEquals(1,service.create(request(menu.getId(),1)).queue().queueNumber());
 }
}
