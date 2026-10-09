package com.kku.queuenotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.repository.MenuItemRepository;
import com.kku.queuenotify.service.NotificationStrategy;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full application/filters/PostgreSQL; only provider transport strategy is test-only. */
@SpringBootTest(properties={"notification.mode=webpush","staff.username=push-test-operator","staff.password=push-test-password-123"})
@AutoConfigureMockMvc
abstract class PushApiTestSupport {
  static final PostgresTestDatabase DATABASE=start();
  static PostgresTestDatabase start(){try{var database=PostgresTestDatabase.start();
    Runtime.getRuntime().addShutdownHook(new Thread(()->{try{database.close();}catch(Exception ignored){}}));return database;
  }catch(Exception error){throw new IllegalStateException(error);}}
  @DynamicPropertySource static void database(DynamicPropertyRegistry r){
    r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);
  }
  @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired MenuItemRepository menus;
  @MockBean NotificationStrategy provider;
  @BeforeEach void resetDatabase(){reset(provider);jdbc.execute("TRUNCATE TABLE orders,menu_item,queue_daily_counter,push_subscription RESTART IDENTITY CASCADE");}
  record Token(MockHttpSession session,String header,String value){}
  record Owner(Long id,String token){}
  Token csrf(MockHttpSession session) throws Exception {
    var request=get("/api/v1/csrf");if(session!=null)request.session(session);
    var result=mvc.perform(request).andExpect(status().isOk()).andReturn();var body=json.readTree(result.getResponse().getContentAsString());
    return new Token((MockHttpSession)result.getRequest().getSession(false),body.get("headerName").asText(),body.get("token").asText());
  }
  Token staff() throws Exception {
    var token=csrf(null);var login=mvc.perform(post("/staff/login").session(token.session()).header(token.header(),token.value())
        .param("username","push-test-operator").param("password","push-test-password-123"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff")).andReturn();
    return csrf((MockHttpSession)login.getRequest().getSession(false));
  }
  Owner create() throws Exception {
    var menu=menus.saveAndFlush(MenuItem.builder().name("Push API meal").price(new BigDecimal("10.00")).build());var token=csrf(null);
    var result=mvc.perform(post("/api/v1/orders").session(token.session()).header(token.header(),token.value())
        .contentType("application/json").content("{\"items\":[{\"menuItemId\":"+menu.getId()+",\"quantity\":1}]}"))
        .andExpect(status().isCreated()).andReturn();var body=json.readTree(result.getResponse().getContentAsString());
    return new Owner(body.get("id").asLong(),body.get("queueToken").asText());
  }
  PushSubscriptionRequest subscription(){return new PushSubscriptionRequest("https://fcm.googleapis.com/wp/full-api-test",
      new PushSubscriptionRequest.Keys(PushConfigurationTest.KEY,OrderSubscriptionServiceTest.AUTH));}
  void attach(Owner order,Token token) throws Exception {
    mvc.perform(post("/api/v1/orders/"+order.id()+"/subscription").session(token.session()).header(token.header(),token.value())
        .header("X-Queue-Token",order.token()).contentType("application/json").content(json.writeValueAsString(subscription())))
        .andExpect(status().isNoContent());
  }
  void advance(Owner order,Token staff,String expected) throws Exception {
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/advance").session(staff.session()).header(staff.header(),staff.value()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(expected));
  }
  int logs(Owner order){return jdbc.queryForObject("SELECT count(*) FROM notification_log WHERE queue_id=?",Integer.class,order.id());}
}
