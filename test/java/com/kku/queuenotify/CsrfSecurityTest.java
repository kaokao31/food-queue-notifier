package com.kku.queuenotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.config.SecurityConfig;
import com.kku.queuenotify.controller.api.CsrfController;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.impl.OrderAccessServiceImpl;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CsrfSecurityTest {
  // Only persistence and route probes are fixtures. Security, CSRF endpoint and owner matching are real.
  static final String OWNER="owner-fixture-only";
  @Configuration(proxyBeanMethods=false) @EnableWebMvc @Import({CsrfController.class,Probe.class})
  static class TestMvc {
    @Bean AtomicInteger mutations(){return new AtomicInteger();}
    @Bean OrderAccessService actualAccess(){
      var tokens=new QueueToken();var queue=new Queue();queue.setId(8L);queue.setTokenHash(tokens.hash(OWNER));
      var repository=mock(QueueRepository.class);when(repository.lockById(8L)).thenReturn(Optional.of(queue));
      return new OrderAccessServiceImpl(repository,tokens);
    }
  }
  @RestController static class Probe {
    final OrderAccessService access;final AtomicInteger mutations;
    Probe(OrderAccessService access,AtomicInteger mutations){this.access=access;this.mutations=mutations;}
    @PostMapping("/api/v1/orders") String create(){mutations.incrementAndGet();return "CREATED";}
    @RequestMapping(value={"/api/v1/orders/{id}","/api/v1/orders/{id}/subscription","/api/v1/queues/{id}/cancel"},
        method={RequestMethod.PUT,RequestMethod.POST,RequestMethod.DELETE,RequestMethod.PATCH})
    String owned(@PathVariable Long id,@RequestHeader(value="X-Queue-Token",required=false) String token){
      access.locked(id,token);mutations.incrementAndGet();return "OWNED";
    }
    @RequestMapping(value={"/api/v1/menu-items","/api/v1/queues/{id}/advance"},method={RequestMethod.POST,RequestMethod.PATCH})
    String staff(){assertTrue(access.isStaff());mutations.incrementAndGet();return "STAFF";}
    @ExceptionHandler(ApiException.class) ResponseEntity<Map<String,String>> error(ApiException e){
      return ResponseEntity.status(e.getStatus()).body(Map.of("message",e.getMessage()));
    }
  }
  interface Check {void run(MockMvc mvc,AtomicInteger mutations) throws Exception;}
  void check(Check test){new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class))
      .withUserConfiguration(SecurityConfig.class,TestMvc.class)
      .withPropertyValues("staff.username=operator","staff.password=test-only-password-123")
      .run(ctx->{assertNull(ctx.getStartupFailure());test.run(MockMvcBuilders.webAppContextSetup(ctx)
          .apply(springSecurity()).build(),ctx.getBean(AtomicInteger.class));});}
  record Token(MockHttpSession session,String header,String value) {}
  Token token(MockMvc mvc,MockHttpSession session) throws Exception {
    var request=get("/api/v1/csrf");if(session!=null)request.session(session);
    var result=mvc.perform(request).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
        .andExpect(jsonPath("$.parameterName").value("_csrf")).andReturn();
    var json=new ObjectMapper().readTree(result.getResponse().getContentAsString());
    assertFalse(json.get("token").asText().isBlank());
    return new Token((MockHttpSession)result.getRequest().getSession(false),json.get("headerName").asText(),json.get("token").asText());
  }
  @Test void browserCanFetchTokenAndCreateWithReturnedHeaderInSameSession(){check((mvc,calls)->{
    var token=token(mvc,null);assertNotNull(token.session());
    mvc.perform(post("/api/v1/orders").session(token.session()).header(token.header(),token.value()))
        .andExpect(status().isOk()).andExpect(content().string("CREATED"));assertEquals(1,calls.get());
    var refreshed=token(mvc,token.session());assertEquals(token.session(),refreshed.session());
    mvc.perform(post("/api/v1/orders").session(token.session()).header(refreshed.header(),refreshed.value())).andExpect(status().isOk());
  });}
  @Test void missingInvalidOrOtherSessionTokenFailsBeforeMutation(){check((mvc,calls)->{
    var one=token(mvc,null);var two=token(mvc,null);
    mvc.perform(post("/api/v1/orders").session(one.session())).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/orders").session(one.session()).header(one.header(),"invalid")).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/orders").session(two.session()).header(one.header(),one.value())).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/orders").header(one.header(),one.value())).andExpect(status().isForbidden());
    assertEquals(0,calls.get());
  });}
  @Test void csrfDoesNotReplaceOwnerTokenForExistingOrderMutations(){check((mvc,calls)->{
    var token=token(mvc,null);
    for(var route:new String[][]{{"PUT","/api/v1/orders/8"},{"POST","/api/v1/orders/8/subscription"},
        {"DELETE","/api/v1/orders/8/subscription"},{"PATCH","/api/v1/queues/8/cancel"}}){
      mvc.perform(request(HttpMethod.valueOf(route[0]),route[1]).session(token.session()).header(token.header(),token.value()))
          .andExpect(status().isForbidden());
      mvc.perform(request(HttpMethod.valueOf(route[0]),route[1]).session(token.session()).header(token.header(),token.value()).header("X-Queue-Token","wrong"))
          .andExpect(status().isForbidden());
      mvc.perform(request(HttpMethod.valueOf(route[0]),route[1]).session(token.session()).header(token.header(),token.value()).header("X-Queue-Token",OWNER))
          .andExpect(status().isOk()).andExpect(content().string("OWNED"));
    }
    assertEquals(4,calls.get());
  });}
  @Test void ownerTokenWithoutCsrfCannotMutate(){check((mvc,calls)->{
    mvc.perform(put("/api/v1/orders/8").header("X-Queue-Token",OWNER)).andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/orders/8/subscription").header("X-Queue-Token",OWNER)).andExpect(status().isForbidden());
    assertEquals(0,calls.get());
  });}
  @Test void ownerAndCsrfCannotManageMenuDeleteOrderAdvanceOrReadLogs(){check((mvc,calls)->{
    var token=token(mvc,null);
    for(var route:new String[][]{{"POST","/api/v1/menu-items"},{"DELETE","/api/v1/orders/8"},{"PATCH","/api/v1/queues/8/advance"}})
      mvc.perform(request(HttpMethod.valueOf(route[0]),route[1]).session(token.session()).header(token.header(),token.value()).header("X-Queue-Token",OWNER))
          .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/orders/8/notifications").session(token.session()).header("X-Queue-Token",OWNER)).andExpect(status().isUnauthorized());
    assertEquals(0,calls.get());
  });}
  @Test void loginRotatesCsrfAndNewTokenAllowsStaffMutation(){check((mvc,calls)->{
    var before=token(mvc,null);
    var result=mvc.perform(post("/staff/login").session(before.session()).header(before.header(),before.value())
        .param("username","operator").param("password","test-only-password-123"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff")).andReturn();
    var session=(MockHttpSession)result.getRequest().getSession(false);
    mvc.perform(post("/api/v1/menu-items").session(session).header(before.header(),before.value())).andExpect(status().isForbidden());
    var after=token(mvc,session);
    mvc.perform(post("/api/v1/menu-items").session(session).header(after.header(),after.value()))
        .andExpect(status().isOk()).andExpect(content().string("STAFF"));assertEquals(1,calls.get());
    mvc.perform(post("/staff/logout").session(session).header(after.header(),after.value()))
        .andExpect(status().isFound());assertTrue(session.isInvalid());
    mvc.perform(post("/api/v1/orders").header(after.header(),after.value())).andExpect(status().isForbidden());
    var fresh=token(mvc,null);
    mvc.perform(post("/api/v1/orders").session(fresh.session()).header(fresh.header(),fresh.value())).andExpect(status().isOk());
  });}
}
