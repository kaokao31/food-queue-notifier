package com.kku.queuenotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.OrderResponse;
import com.kku.queuenotify.repository.MenuItemRepository;
import com.kku.queuenotify.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL, security chain, configured BCrypt login, CSRF endpoint and queue/order services. */
@SpringBootTest(properties={"staff.username=queue-test-operator","staff.password=queue-test-password-123"})
@AutoConfigureMockMvc
class QueueApiIntegrationTest {
  static final PostgresTestDatabase DATABASE=start();
  static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
  @DynamicPropertySource static void database(DynamicPropertyRegistry r){
    r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);
    r.add("spring.datasource.password",DATABASE::password);
  }
  @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired OrderService orders;
  @Autowired MenuItemRepository menus;@Autowired JdbcTemplate jdbc;
  @BeforeEach void reset(){jdbc.execute("TRUNCATE TABLE orders,menu_item,queue_daily_counter RESTART IDENTITY CASCADE");}
  @AfterAll static void close() throws Exception {DATABASE.close();}
  OrderResponse create(){var menu=menus.saveAndFlush(MenuItem.builder().name("API test meal").price(new BigDecimal("10.00")).build());
    return orders.create(new OrderRequest(List.of(new OrderRequest.Item(menu.getId(),1))));}
  record Csrf(MockHttpSession session,String header,String value){}
  Csrf csrf(MockHttpSession session) throws Exception {
    var request=get("/api/v1/csrf");if(session!=null)request.session(session);
    var result=mvc.perform(request).andExpect(status().isOk()).andReturn();
    var body=json.readTree(result.getResponse().getContentAsString());
    return new Csrf((MockHttpSession)result.getRequest().getSession(false),body.get("headerName").asText(),body.get("token").asText());
  }
  MockHttpSession login() throws Exception {
    var token=csrf(null);
    var result=mvc.perform(post("/staff/login").session(token.session()).header(token.header(),token.value())
        .param("username","queue-test-operator").param("password","queue-test-password-123"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff")).andReturn();
    return (MockHttpSession)result.getRequest().getSession(false);
  }
  @Test void ownerTrackingRejectsMissingWrongAndOtherBillToken() throws Exception {
    var first=create();var second=create();
    mvc.perform(get("/api/v1/queues/"+first.id()).header("X-Queue-Token",first.queueToken()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING"))
        .andExpect(header().string("Cache-Control","no-store"))
        .andExpect(jsonPath("$.tokenHash").doesNotExist()).andExpect(jsonPath("$.queueToken").doesNotExist());
    mvc.perform(get("/api/v1/queues/"+first.id())).andExpect(status().isForbidden());
    for(var token:List.of("wrong",second.queueToken()))mvc.perform(get("/api/v1/queues/"+first.id())
        .header("X-Queue-Token",token)).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/queues/999999").header("X-Queue-Token",first.queueToken())).andExpect(status().isNotFound());
  }
  @Test void configuredStaffSessionAdvancesLifecycleAndRejectsTerminalMutation() throws Exception {
    var order=create();var session=login();var token=csrf(session);
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/advance").session(session)).andExpect(status().isForbidden());
    for(var state:List.of("PREPARING","READY","COMPLETED")){
      mvc.perform(patch("/api/v1/queues/"+order.id()+"/advance").session(session).header(token.header(),token.value()))
          .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
      assertEquals(state,jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    }
    mvc.perform(get("/api/v1/queues/"+order.id()).session(session)).andExpect(status().isOk());
    for(var action:List.of("advance","cancel"))mvc.perform(patch("/api/v1/queues/"+order.id()+"/"+action)
        .session(session).header(token.header(),token.value())).andExpect(status().isConflict());
    assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification_log",Integer.class));
    assertEquals("PREVIEW",jdbc.queryForObject("SELECT delivery_status FROM notification_log",String.class)); // T13 console observer is now active.
  }
  @Test void customerCancelRequiresCsrfAndOwnerTokenAndCommitsCancellation() throws Exception {
    var order=create();var token=csrf(null);
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/cancel").header("X-Queue-Token",order.queueToken()))
        .andExpect(status().isForbidden());
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/cancel").session(token.session())
        .header(token.header(),token.value()).header("X-Queue-Token","wrong")).andExpect(status().isForbidden());
    assertEquals("WAITING",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/cancel").session(token.session())
        .header(token.header(),token.value()).header("X-Queue-Token",order.queueToken()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
  }
  @Test void customerCannotAdvanceAndOwnerCannotCancelReadyQueue() throws Exception {
    var order=create();var customer=csrf(null);
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/advance").session(customer.session())
        .header(customer.header(),customer.value()).header("X-Queue-Token",order.queueToken())).andExpect(status().isUnauthorized());
    var staff=csrf(login());
    for(int i=0;i<2;i++)mvc.perform(patch("/api/v1/queues/"+order.id()+"/advance").session(staff.session())
        .header(staff.header(),staff.value())).andExpect(status().isOk());
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/cancel").session(customer.session())
        .header(customer.header(),customer.value()).header("X-Queue-Token",order.queueToken())).andExpect(status().isConflict());
    assertEquals("READY",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
  }
}
