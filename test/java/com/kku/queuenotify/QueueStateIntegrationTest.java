package com.kku.queuenotify;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.OrderService;
import com.kku.queuenotify.service.QueueService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL locks and application services. HTTP cases log in through the real filters.
 * Service concurrency cases use the existing test-only STAFF servlet request in each worker.
 * No subscription is attached, so READY must not call the inherited provider transport fixture. */
class QueueStateIntegrationTest extends PushApiTestSupport {
  @Autowired QueueService queues;
  @Autowired OrderService orders;
  @Autowired PlatformTransactionManager transactions;
  @AfterEach void clearRequest(){RequestContextHolder.resetRequestAttributes();}
  String state(Owner owner){return jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,owner.id());}
  OrderRequest edit(Owner owner,int quantity){
    Long menu=jdbc.queryForObject("SELECT menu_item_id FROM order_item WHERE order_id=?",Long.class,owner.id());
    return new OrderRequest(List.of(new OrderRequest.Item(menu,quantity)));
  }
  void assertConflict(Runnable operation){assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,operation::run).getStatus());}
  @Test void editAndDeleteRulesHoldAcrossEveryPersistedState() throws Exception {
    QueueServiceTest.staff();
    for(var target:QueueStatus.values()){
      var order=create();
      switch(target){
        case WAITING -> {}
        case PREPARING -> queues.advance(order.id());
        case READY -> {queues.advance(order.id());queues.advance(order.id());}
        case COMPLETED -> {queues.advance(order.id());queues.advance(order.id());queues.advance(order.id());}
        case CANCELLED -> queues.cancel(order.id(),null);
      }
      assertEquals(target.name(),state(order));
      if(target==QueueStatus.WAITING){
        orders.update(order.id(),null,edit(order,2));assertEquals(2,orders.get(order.id(),null).items().get(0).quantity());
        orders.delete(order.id());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM orders WHERE id=?",Integer.class,order.id()));
      }else{
        var before=orders.get(order.id(),null);
        assertConflict(()->orders.update(order.id(),null,edit(order,2)));assertConflict(()->orders.delete(order.id()));
        assertEquals(target.name(),state(order));assertEquals(before.totalAmount(),orders.get(order.id(),null).totalAmount());
        assertEquals(1,orders.get(order.id(),null).items().get(0).quantity());
      }
    }
    verifyNoInteractions(provider);
  }
  @Test void terminalApiRequestsCannotChangeStatusTimestampOrOrderItems() throws Exception {
    var completed=create();var cancelled=create();var staff=staff();
    advance(completed,staff,"PREPARING");advance(completed,staff,"READY");advance(completed,staff,"COMPLETED");
    var customer=csrf(null);
    mvc.perform(patch("/api/v1/queues/"+cancelled.id()+"/cancel").session(customer.session()).header(customer.header(),customer.value())
        .header("X-Queue-Token",cancelled.token())).andExpect(status().isOk());
    for(var order:List.of(completed,cancelled)){
      var before=jdbc.queryForMap("SELECT status,status_changed_at FROM queue WHERE id=?",order.id());
      for(String action:List.of("advance","cancel"))
        mvc.perform(patch("/api/v1/queues/"+order.id()+"/"+action).session(staff.session()).header(staff.header(),staff.value()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(header().string("Cache-Control","no-store"));
      assertEquals(before,jdbc.queryForMap("SELECT status,status_changed_at FROM queue WHERE id=?",order.id()));
      assertEquals(1,jdbc.queryForObject("SELECT quantity FROM order_item WHERE order_id=?",Integer.class,order.id()));
      assertEquals(0,logs(order));
    }
    verifyNoInteractions(provider);
  }
  /** First mutation holds the queue lock in an outer transaction; second must recheck after commit. */
  void lockedRace(Owner order,Runnable first,Callable<QueueResponse> second,String expected) throws Exception {
    var entered=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
    final Future<?>[] waiting=new Future<?>[1];QueueServiceTest.staff();
    try {
      new TransactionTemplate(transactions).executeWithoutResult(tx->{
        first.run();
        waiting[0]=pool.submit(()->{
          QueueServiceTest.staff();entered.countDown();
          try{return second.call();}finally{RequestContextHolder.resetRequestAttributes();}
        });
        try{assertTrue(entered.await(5,TimeUnit.SECONDS));
          assertThrows(TimeoutException.class,()->waiting[0].get(250,TimeUnit.MILLISECONDS));
        }catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
      });
      var failed=assertThrows(ExecutionException.class,()->waiting[0].get(10,TimeUnit.SECONDS));
      assertInstanceOf(ApiException.class,failed.getCause());assertEquals(HttpStatus.CONFLICT,((ApiException)failed.getCause()).getStatus());
      assertEquals(expected,state(order));assertEquals(0,logs(order));verifyNoInteractions(provider);
    }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  }
  @Test void cancellationCommitMakesWaitingAdvanceFailRatherThanReviveQueue() throws Exception {
    var order=create();lockedRace(order,()->queues.cancel(order.id(),null),()->queues.advance(order.id()),"CANCELLED");
  }
  @Test void readyCommitMakesWaitingCancellationFailRatherThanUndoReady() throws Exception {
    var order=create();QueueServiceTest.staff();queues.advance(order.id());
    lockedRace(order,()->queues.advance(order.id()),()->queues.cancel(order.id(),null),"READY");
  }
  @Test void completionCommitMakesDuplicateAdvanceFailRatherThanAdvanceTerminalState() throws Exception {
    var order=create();QueueServiceTest.staff();queues.advance(order.id());queues.advance(order.id());
    lockedRace(order,()->queues.advance(order.id()),()->queues.advance(order.id()),"COMPLETED");
  }
  @Test void twoConcurrentAdvanceRequestsSerializeAsTwoLegalSteps() throws Exception {
    var order=create();var start=new CountDownLatch(1);var ready=new CountDownLatch(2);var pool=Executors.newFixedThreadPool(2);
    Callable<QueueStatus> advance=()->{
      QueueServiceTest.staff();ready.countDown();
      try{assertTrue(start.await(5,TimeUnit.SECONDS));return queues.advance(order.id()).status();}
      finally{RequestContextHolder.resetRequestAttributes();}
    };
    try{
      var one=pool.submit(advance);var two=pool.submit(advance);assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
      assertEquals(Set.of(QueueStatus.PREPARING,QueueStatus.READY),Set.of(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS)));
      assertEquals("READY",state(order));assertEquals(0,logs(order));verifyNoInteractions(provider);
    }finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  }
}
