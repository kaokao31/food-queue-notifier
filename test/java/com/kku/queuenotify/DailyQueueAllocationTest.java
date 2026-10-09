package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.repository.DailyQueueCounterRepository;
import com.kku.queuenotify.service.QueueNumberService;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
class DailyQueueAllocationTest {
 static final PostgresTestDatabase DATABASE=start();
 static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);}
 @AfterAll static void close() throws Exception {DATABASE.close();}
 @Autowired DailyQueueCounterRepository counters;@Autowired QueueNumberService service;@Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;
 @BeforeEach void clearIsolatedCounterFixtures(){jdbc.update("DELETE FROM queue_daily_counter");}
 int next(LocalDate day){return new TransactionTemplate(manager).execute(s->counters.incrementAndGet(day));}
 @Test void requiresAnExistingTransactionForBothEntryPoints(){
  assertThrows(IllegalTransactionStateException.class,()->counters.incrementAndGet(LocalDate.of(2026,10,9)));assertThrows(IllegalTransactionStateException.class,service::next);
  var value=new TransactionTemplate(manager).execute(s->service.next());assertNotNull(value.date());assertEquals(1,value.value());
 }
 @Test void resetsByDaySupports1000AndRollsBackBothInsertAndIncrement(){
  var day=LocalDate.of(2026,10,9);jdbc.update("INSERT INTO queue_daily_counter(queue_date,last_number) VALUES (?,999)",day);
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{assertEquals(1000,counters.incrementAndGet(day));throw new IllegalStateException("rollback fixture");}));assertEquals(1000,next(day));assertEquals(1001,next(day));
  var tomorrow=day.plusDays(1);assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{assertEquals(1,counters.incrementAndGet(tomorrow));throw new IllegalStateException("rollback fixture");}));assertEquals(1,next(tomorrow));assertEquals(1002,next(day));
 }
 @Test void concurrentTransactionsReceiveDistinctConsecutiveNumbers() throws Exception {
  var day=LocalDate.of(2026,10,9);var ready=new CountDownLatch(8);var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(8);var futures=new ArrayList<Future<Integer>>();
  try{
   for(int i=0;i<8;i++)futures.add(pool.submit(()->{ready.countDown();if(!go.await(15,TimeUnit.SECONDS))throw new IllegalStateException("start timeout");return next(day);}));
   assertTrue(ready.await(15,TimeUnit.SECONDS));go.countDown();var numbers=new TreeSet<Integer>();for(var future:futures)numbers.add(future.get(30,TimeUnit.SECONDS));assertEquals(new TreeSet<>(List.of(1,2,3,4,5,6,7,8)),numbers);assertEquals(9,next(day));
  }finally{go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
 }
}
