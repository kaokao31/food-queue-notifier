package com.kku.queuenotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.config.SecurityConfig;
import com.kku.queuenotify.controller.api.CsrfController;
import com.kku.queuenotify.controller.api.QueueController;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.exception.GlobalExceptionHandler;
import com.kku.queuenotify.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real BCrypt form login, session, CSRF endpoint, filters and queue controller.
 * Only QueueService is a test double; PostgreSQL behavior is covered in the persistence suites.
 * Requests use returned CSRF tokens, without injected users or CSRF post-processors. */
class StaffSecurityIntegrationTest {
  private static final String PASSWORD="integration-only-password-123";
  @TestConfiguration(proxyBeanMethods=false) @EnableWebMvc
  @Import({CsrfController.class,QueueController.class,GlobalExceptionHandler.class})
  static class Web { @Bean QueueService queues(){return mock(QueueService.class);} }
  interface Check {void run(MockMvc mvc,QueueService queues) throws Exception;}
  void check(Check test){new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class))
      .withUserConfiguration(SecurityConfig.class,Web.class)
      .withPropertyValues("staff.username=operator","staff.password="+PASSWORD)
      .run(ctx->{assertNull(ctx.getStartupFailure());
        var queues=ctx.getBean(QueueService.class);
        when(queues.advance(8L)).thenAnswer(call->{
          var auth=SecurityContextHolder.getContext().getAuthentication();
          assertNotNull(auth);assertEquals("operator",auth.getName());
          assertTrue(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_STAFF")));
          return new QueueResponse(8L,3,null,QueueStatus.PREPARING,null);
        });
        test.run(MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build(),queues);
      });}
  record Token(MockHttpSession session,String header,String value){}
  Token token(MockMvc mvc,MockHttpSession session) throws Exception {
    var request=get("/api/v1/csrf");if(session!=null)request.session(session);
    var result=mvc.perform(request).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control","no-store")).andReturn();
    var body=new ObjectMapper().readTree(result.getResponse().getContentAsString());
    assertEquals("X-CSRF-TOKEN",body.get("headerName").asText());
    assertFalse(body.get("token").asText().isBlank());
    return new Token((MockHttpSession)result.getRequest().getSession(false),body.get("headerName").asText(),body.get("token").asText());
  }
  Token login(MockMvc mvc,Token before) throws Exception {
    String oldId=before.session().getId();
    mvc.perform(post("/staff/login").session(before.session()).header(before.header(),before.value())
        .param("username","operator").param("password",PASSWORD))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff"));
    assertNotEquals(oldId,before.session().getId());
    return token(mvc,before.session());
  }
  void advance(MockMvc mvc,Token token,int expected) throws Exception {
    mvc.perform(patch("/api/v1/queues/8/advance").session(token.session()).header(token.header(),token.value()))
        .andExpect(status().is(expected)).andExpect(header().string("Cache-Control","no-store"));
  }
  @Test void anonymousCsrfDoesNotGrantStaffAccess() {check((mvc,queues)->{
    var anonymous=token(mvc,null);
    mvc.perform(patch("/api/v1/queues/8/advance").session(anonymous.session()).header(anonymous.header(),anonymous.value()))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized")).andExpect(header().string("Cache-Control","no-store"));
    verifyNoInteractions(queues);
  });}
  @Test void successfulLoginRotatesTokenAndOnlyFreshTokenReachesQueueController(){check((mvc,queues)->{
    var before=token(mvc,null);var after=login(mvc,before);
    advance(mvc,before,403);verifyNoInteractions(queues);
    advance(mvc,after,200);verify(queues).advance(8L);
  });}
  @Test void failedLoginCanRetryWithFetchedTokenWithoutGainingAccess(){check((mvc,queues)->{
    var before=token(mvc,null);
    mvc.perform(post("/staff/login").session(before.session()).header(before.header(),before.value())
        .param("username","operator").param("password","wrong-test-only"))
        .andExpect(redirectedUrl("/staff/login?error"));
    var retry=token(mvc,before.session());advance(mvc,retry,401);verifyNoInteractions(queues);
    advance(mvc,login(mvc,retry),200);verify(queues).advance(8L);
  });}
  @Test void loggedInSessionRejectsMissingMalformedAndOtherSessionsTokens(){check((mvc,queues)->{
    var one=login(mvc,token(mvc,null));var two=login(mvc,token(mvc,null));
    mvc.perform(patch("/api/v1/queues/8/advance").session(one.session())).andExpect(status().isForbidden());
    for(String invalid:new String[]{"invalid",two.value()})
      mvc.perform(patch("/api/v1/queues/8/advance").session(one.session()).header(one.header(),invalid))
          .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403)).andExpect(jsonPath("$.error").value("Forbidden"));
    verifyNoInteractions(queues);advance(mvc,one,200);verify(queues).advance(8L);
  });}
  @Test void logoutInvalidatesOnlyItsSessionAndOldTokenCannotBeReplayed(){check((mvc,queues)->{
    var one=login(mvc,token(mvc,null));var two=login(mvc,token(mvc,null));
    mvc.perform(post("/staff/logout").session(one.session()).header(one.header(),one.value()))
        .andExpect(redirectedUrl("/staff/login?logout"));assertTrue(one.session().isInvalid());assertFalse(two.session().isInvalid());
    mvc.perform(patch("/api/v1/queues/8/advance").header(one.header(),one.value())).andExpect(status().isForbidden());
    var fresh=token(mvc,null);advance(mvc,fresh,401);verifyNoInteractions(queues);
    advance(mvc,two,200);verify(queues).advance(8L);
  });}
  @Test void rejectedLogoutPreservesLoginAndFreshTokenStillWorks(){check((mvc,queues)->{
    var staff=login(mvc,token(mvc,null));
    mvc.perform(post("/staff/logout").session(staff.session()).header(staff.header(),"invalid"))
        .andExpect(status().isForbidden());assertFalse(staff.session().isInvalid());
    var refreshed=token(mvc,staff.session());advance(mvc,refreshed,200);verify(queues).advance(8L);
  });}
}
