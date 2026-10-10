package com.kku.queuenotify;
import com.kku.queuenotify.support.OrderingTestDoubles;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
/** Isolated PostgreSQL and test-only access/token/clock providers. No C/T implementation. */
@SpringBootTest
// Existing A/T business fixtures do not establish security; real filters are exercised by SecurityRoutesTest.
@AutoConfigureMockMvc(addFilters=false)
@Import({OrderingTestDoubles.class,IntegrationTestSupport.TimeFixture.class})
abstract class IntegrationTestSupport {
 static final PostgresTestDatabase DATABASE=start();
 static PostgresTestDatabase start(){try{var db=PostgresTestDatabase.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{try{db.close();}catch(Exception ignored){}}));return db;}catch(Exception e){throw new IllegalStateException(e);}}
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);}
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired OrderingTestDoubles.Access access;@Autowired OrderingTestDoubles.Tokens tokens;@Autowired MutableClock time;
 @BeforeEach void resetIsolatedFixtures(){access.staff=false;tokens.failHash=false;time.now.set(Instant.parse("2026-10-09T16:59:59Z"));jdbc.execute("TRUNCATE TABLE orders,menu_item,queue_daily_counter RESTART IDENTITY CASCADE");}
 @TestConfiguration(proxyBeanMethods=false) static class TimeFixture {@Bean @Primary MutableClock integrationClock(){return new MutableClock();}}
 static class MutableClock extends Clock {
  final AtomicReference<Instant> now=new AtomicReference<>(Instant.parse("2026-10-09T16:59:59Z"));
  public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}public Instant instant(){return now.get();}
 }
}
