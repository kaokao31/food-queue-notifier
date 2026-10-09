package com.kku.queuenotify;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.config.SecurityConfig;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.impl.OrderAccessServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityRoutesTest {
  // Route probes and authenticated users are tests only. The production filter chain and C03 role check are real.
  @Configuration(proxyBeanMethods=false) @EnableWebMvc @Import(Probe.class) static class TestMvc {
    @Bean OrderAccessService actualAccess(){return new OrderAccessServiceImpl(mock(QueueRepository.class),new QueueToken());}
  }
  @RestController static class Probe {
    private final OrderAccessService access;
    Probe(OrderAccessService access){this.access=access;}
    @RequestMapping("/**") public String route(){return access.isStaff()?"STAFF":"PUBLIC";}
  }
  interface Check {void run(MockMvc mvc,org.springframework.boot.test.context.assertj.AssertableWebApplicationContext context) throws Exception;}
  void check(Check check) {
    new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class))
        .withUserConfiguration(SecurityConfig.class,TestMvc.class).run(ctx->{assertNull(ctx.getStartupFailure());
          check.run(MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build(),ctx);
        });
  }
  @Test void publicReadRoutesStayAvailableAndKeepSecurityHeaders(){check((mvc,ctx)->{
    for(String path:new String[]{"/","/queue/8","/staff/login","/assets/app.js","/sw.js","/api/v1/menu-items","/api/v1/menu-items/8/image","/api/v1/push/public-key","/actuator/health"})
      mvc.perform(get(path)).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff"));
    mvc.perform(get("/")).andExpect(header().string("Referrer-Policy","same-origin"));
  });}
  @Test void protectedApiReturns401AndUiRedirectsForAnonymous(){check((mvc,ctx)->{
    for(String path:new String[]{"/api/v1/orders","/api/v1/orders/8/notifications"})mvc.perform(get(path)).andExpect(status().isUnauthorized())
        .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.status").value(401));
    mvc.perform(get("/staff")).andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost/staff/login"));
  });}
  @Test void authenticatedNonStaffIsForbiddenAndStaffRoleReachesC03Check(){check((mvc,ctx)->{
    mvc.perform(get("/api/v1/orders/8/notifications").with(user("test-only").roles("CUSTOMER"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/orders/8/notifications").with(user("test-only").roles("STAFF"))).andExpect(status().isOk()).andExpect(content().string("STAFF"));
    mvc.perform(get("/staff/menu").with(user("test-only").roles("STAFF"))).andExpect(status().isOk());
  });}
  @Test void customerReadRoutingDoesNotGiveAnOwnerStaffRole(){check((mvc,ctx)->{
    mvc.perform(get("/api/v1/orders/8").header("X-Queue-Token","owner-fixture")).andExpect(status().isOk()).andExpect(content().string("PUBLIC"));
    mvc.perform(get("/api/v1/orders/8/notifications").header("X-Queue-Token","owner-fixture")).andExpect(status().isUnauthorized());
  });}
  @Test void csrfStaysEnabledAndMutationsRemainStaffOnlyAtThisStep(){check((mvc,ctx)->{
    for(String path:new String[]{"/api/v1/orders","/api/v1/menu-items","/api/v1/orders/8/subscription"}){
      mvc.perform(post(path)).andExpect(status().isForbidden());
      mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized());
      mvc.perform(post(path).with(user("test-only").roles("STAFF"))).andExpect(status().isForbidden());
      mvc.perform(post(path).with(user("test-only").roles("STAFF")).with(csrf())).andExpect(status().isOk()).andExpect(content().string("STAFF"));
    }
    mvc.perform(delete("/api/v1/orders/8").with(user("test-only").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
    mvc.perform(patch("/api/v1/queues/8/advance").with(user("test-only").roles("STAFF")).with(csrf())).andExpect(status().isOk());
    mvc.perform(post("/api/v1/orders").with(user("test-only").roles("STAFF")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
  });}
  @Test void unknownRoutesAreDeniedEvenToStaff(){check((mvc,ctx)->{
    mvc.perform(get("/api/private/new-endpoint").with(user("test-only").roles("STAFF"))).andExpect(status().isForbidden());
    mvc.perform(get("/actuator/env").with(user("test-only").roles("STAFF"))).andExpect(status().isForbidden());
  });}
  @Test void noGeneratedDefaultUserOrWorkingBasicAuthentication(){check((mvc,ctx)->{
    assertTrue(ctx.getBeansOfType(UserDetailsService.class).isEmpty());
    var provider=ctx.getBean(AuthenticationProvider.class);
    assertThrows(BadCredentialsException.class,()->provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("staff","test-only")));
    mvc.perform(get("/api/v1/orders").with(httpBasic("staff","test-only"))).andExpect(status().isUnauthorized());
  });}
}
