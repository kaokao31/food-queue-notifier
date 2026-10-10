package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.dto.request.*;
import com.kku.queuenotify.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
class DailyQueueIntegrationTest extends IntegrationTestSupport {
 @Autowired OrderService orders;@Autowired MenuService menus;
 OrderRequest request(){var menu=menus.create(new MenuItemRequest("Rice","FOOD",BigDecimal.TEN,10,true));return new OrderRequest(List.of(new OrderRequest.Item(menu.id(),1)));}
 @Test void actualOrdersResetAtBangkokMidnightAndFailedOrderDoesNotConsumeNumber(){
  var request=request();var first=orders.create(request);assertEquals(LocalDate.of(2026,10,9),first.queue().queueDate());assertEquals(1,first.queue().queueNumber());
  time.now.set(Instant.parse("2026-10-09T17:00:00Z"));tokens.failHash=true;assertThrows(IllegalStateException.class,()->orders.create(request));tokens.failHash=false;
  var tomorrow=orders.create(request);assertEquals(LocalDate.of(2026,10,10),tomorrow.queue().queueDate());assertEquals(1,tomorrow.queue().queueNumber());assertEquals(2,orders.create(request).queue().queueNumber());
 }
 @Test void concurrentOrderTransactionsPersistDistinctNumbersAndTokens() throws Exception {
  var request=request();var pool=Executors.newFixedThreadPool(6);var go=new CountDownLatch(1);var ready=new CountDownLatch(6);var results=new ArrayList<Future<com.kku.queuenotify.dto.response.OrderResponse>>();
  try{for(int i=0;i<6;i++)results.add(pool.submit(()->{ready.countDown();if(!go.await(15,TimeUnit.SECONDS))throw new IllegalStateException("timeout");return orders.create(request);}));assertTrue(ready.await(15,TimeUnit.SECONDS));go.countDown();var numbers=new TreeSet<Integer>();var secrets=new HashSet<String>();var ids=new HashSet<Long>();for(var result:results){var order=result.get(30,TimeUnit.SECONDS);numbers.add(order.queue().queueNumber());secrets.add(order.queueToken());ids.add(order.id());}assertEquals(new TreeSet<>(List.of(1,2,3,4,5,6)),numbers);assertEquals(6,secrets.size());assertEquals(6,ids.size());assertEquals(6,jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));}
  finally{go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
 }
}
