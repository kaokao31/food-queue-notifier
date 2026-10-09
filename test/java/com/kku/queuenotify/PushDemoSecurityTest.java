package com.kku.queuenotify;

import com.kku.queuenotify.config.SecurityConfig;
import com.kku.queuenotify.controller.api.PushSubscriptionController;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.service.impl.InMemoryPushSubscriptionService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PushDemoSecurityTest {
  @TestConfiguration(proxyBeanMethods=false) @EnableWebMvc static class Web {
    @Bean SubscriptionValidator validator(){return new SubscriptionValidator();}
    @Bean WebPushSender sender(){return mock(WebPushSender.class);}
    @Bean Clock clock(){return Clock.systemUTC();}
  }
  WebApplicationContextRunner runner(boolean enabled){return new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class))
      .withUserConfiguration(SecurityConfig.class,Web.class,InMemoryPushSubscriptionService.class,PushSubscriptionController.class)
      .withPropertyValues("spring.profiles.active="+(enabled?"push-demo":"default"));}
  interface Check{void run(MockMvc mvc,WebPushSender sender)throws Exception;}
  void check(Check check){runner(true).run(ctx->{assertNull(ctx.getStartupFailure());check.run(MockMvcBuilders.webAppContextSetup(ctx)
      .apply(springSecurity()).build(),ctx.getBean(WebPushSender.class));});}
  String body(){return "{\"endpoint\":\"https://fcm.googleapis.com/wp/security-demo\",\"keys\":{\"p256dh\":\""+
      PushConfigurationTest.KEY+"\",\"auth\":\""+OrderSubscriptionServiceTest.AUTH+"\"}}";}
  @Test void demoBeansAbsentByDefault(){runner(false).run(ctx->{assertNull(ctx.getStartupFailure());
    assertEquals(0,ctx.getBeansOfType(PushSubscriptionService.class).size());assertEquals(0,ctx.getBeansOfType(PushSubscriptionController.class).size());});}
  @Test void disabledProfileDeniesDemoPageAndApiEvenForStaff(){runner(false).run(ctx->{
    var mvc=MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    mvc.perform(get("/push-demo.html").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/push-demo/send").with(user("staff").roles("STAFF")).with(csrf())).andExpect(status().isForbidden());
  });}
  @Test void demoPageRequiresStaffWhenEnabled(){check((mvc,sender)->{
    mvc.perform(get("/push-demo.html")).andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost/staff/login"));
    mvc.perform(get("/push-demo.html").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
    // This isolated MVC context has no static resource handler; 404 proves the STAFF request passed the filter.
    mvc.perform(get("/push-demo.html").with(user("staff").roles("STAFF"))).andExpect(status().isNotFound());
  });}
  @Test void demoProfileCreatesServiceAndController(){runner(true).run(ctx->{assertNull(ctx.getStartupFailure());
    assertEquals(1,ctx.getBeansOfType(PushSubscriptionService.class).size());assertEquals(1,ctx.getBeansOfType(PushSubscriptionController.class).size());});}
  @Test void unauthenticatedAndNonStaffCannotSend(){check((mvc,sender)->{
    mvc.perform(post("/api/v1/push-demo/send").with(csrf())).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/push-demo/send").with(user("customer").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
    verifyNoInteractions(sender);
  });}
  @Test void staffNeedsCsrfForEveryMutation(){check((mvc,sender)->{
    mvc.perform(post("/api/v1/push-demo/subscription").with(user("staff").roles("STAFF")).contentType("application/json").content(body())).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/push-demo/send").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/push-demo/subscription").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());verifyNoInteractions(sender);
  });}
  @Test void registeredSessionCanSendAndForgetWithoutProductionPersistence(){check((mvc,sender)->{
    var session=new MockHttpSession();when(sender.sendTest(any())).thenReturn(201);
    mvc.perform(post("/api/v1/push-demo/subscription").session(session).with(user("staff").roles("STAFF")).with(csrf())
        .contentType("application/json").content(body())).andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/push-demo/send").session(session).with(user("staff").roles("STAFF")).with(csrf()))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
        .andExpect(jsonPath("$.providerStatus").value(201));
    mvc.perform(delete("/api/v1/push-demo/subscription").session(session).with(user("staff").roles("STAFF")).with(csrf())).andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/push-demo/send").session(session).with(user("staff").roles("STAFF")).with(csrf())).andExpect(status().isNotFound());verify(sender,times(1)).sendTest(any());
  });}
  @Test void providerRejectionIs502(){check((mvc,sender)->{
    var session=new MockHttpSession();when(sender.sendTest(any())).thenReturn(410);
    mvc.perform(post("/api/v1/push-demo/subscription").session(session).with(user("staff").roles("STAFF")).with(csrf())
        .contentType("application/json").content(body())).andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/push-demo/send").session(session).with(user("staff").roles("STAFF")).with(csrf()))
        .andExpect(status().isBadGateway()).andExpect(header().string("Cache-Control","no-store"));
  });}
}
