package com.kku.queuenotify;

import com.kku.queuenotify.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StaffLoginTest {
  // Credentials exist only in this test context. Production values come from environment.
  static final String PASSWORD="test-only-password-123";
  WebApplicationContextRunner runner(String password) {
    return new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class))
        .withUserConfiguration(SecurityConfig.class,SecurityRoutesTest.TestMvc.class)
        .withPropertyValues("staff.username=operator","staff.password="+password);
  }
  interface Check {void run(MockMvc mvc) throws Exception;}
  void check(Check test) {runner(PASSWORD).run(ctx->{assertNull(ctx.getStartupFailure());
    test.run(MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build());});}

  @Test void configuredCredentialUsesBcryptAndOnlyStaffRole(){runner(PASSWORD).run(ctx->{
    assertNull(ctx.getStartupFailure());
    var auth=ctx.getBean(AuthenticationProvider.class).authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated("operator",PASSWORD));
    var principal=(UserDetails)auth.getPrincipal();
    assertTrue(principal.getPassword().startsWith("$2"));assertNotEquals(PASSWORD,principal.getPassword());
    assertTrue(ctx.getBean(PasswordEncoder.class).matches(PASSWORD,principal.getPassword()));
    assertEquals(java.util.List.of("ROLE_STAFF"),auth.getAuthorities().stream().map(Object::toString).toList());
  });}
  @Test void loginCreatesSessionAndReachesRealStaffCheck(){check(mvc->{
    var result=mvc.perform(post("/staff/login").with(csrf()).param("username","operator").param("password",PASSWORD))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff")).andReturn();
    var session=(MockHttpSession)result.getRequest().getSession(false);assertNotNull(session);
    mvc.perform(get("/api/v1/orders").session(session)).andExpect(status().isOk()).andExpect(content().string("STAFF"));
  });}
  @Test void wrongPasswordAndUnknownUserFailWithoutStaffSession(){check(mvc->{
    for(String username:new String[]{"operator","unknown"}) {
      var result=mvc.perform(post("/staff/login").with(csrf()).param("username",username).param("password","incorrect"))
          .andExpect(status().isFound()).andExpect(redirectedUrl("/staff/login?error")).andReturn();
      var session=(MockHttpSession)result.getRequest().getSession(false);
      mvc.perform(get("/api/v1/orders").session(session)).andExpect(status().isUnauthorized());
    }
  });}
  @Test void loginRequiresValidCsrfAndBasicRemainsDisabled(){check(mvc->{
    mvc.perform(post("/staff/login").param("username","operator").param("password",PASSWORD)).andExpect(status().isForbidden());
    mvc.perform(post("/staff/login").with(csrf().useInvalidToken()).param("username","operator").param("password",PASSWORD)).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/orders").with(httpBasic("operator",PASSWORD))).andExpect(status().isUnauthorized());
  });}
  @Test void loginChangesExistingSessionIdAndUsesFixedSuccessTarget(){check(mvc->{
    var session=new MockHttpSession();String oldId=session.getId();
    mvc.perform(get("/staff/menu").session(session)).andExpect(status().isFound());
    mvc.perform(post("/staff/login").session(session).with(csrf()).param("username","operator").param("password",PASSWORD))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff"));
    assertNotEquals(oldId,session.getId());
  });}
  @Test void logoutNeedsPostAndCsrfAndInvalidatesSession(){check(mvc->{
    var result=mvc.perform(post("/staff/login").with(csrf()).param("username","operator").param("password",PASSWORD)).andReturn();
    var session=(MockHttpSession)result.getRequest().getSession(false);
    mvc.perform(get("/staff/logout").session(session)).andExpect(status().isOk());assertFalse(session.isInvalid());
    mvc.perform(post("/staff/logout").session(session)).andExpect(status().isForbidden());assertFalse(session.isInvalid());
    mvc.perform(post("/staff/logout").session(session).with(csrf())).andExpect(status().isFound())
        .andExpect(redirectedUrl("/staff/login?logout"));assertTrue(session.isInvalid());
    mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
  });}
  @Test void blankPasswordDisablesAuthenticationWithoutBreakingPublicReads(){runner("").run(ctx->{
    assertNull(ctx.getStartupFailure());
    assertThrows(org.springframework.security.authentication.BadCredentialsException.class,()->
        ctx.getBean(AuthenticationProvider.class).authenticate(UsernamePasswordAuthenticationToken.unauthenticated("operator",PASSWORD)));
    var mvc=MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    mvc.perform(get("/")).andExpect(status().isOk());
    mvc.perform(post("/staff/login").with(csrf()).param("username","operator").param("password",PASSWORD))
        .andExpect(redirectedUrl("/staff/login?error"));
  });}
  @Test void invalidConfiguredPasswordOrUsernameFailsWithoutEchoingSecret(){
    for(String password:new String[]{"too-short", "x".repeat(73), "ก".repeat(25)}) runner(password).run(ctx->{
      assertNotNull(ctx.getStartupFailure());
      Throwable cause=ctx.getStartupFailure();while(cause.getCause()!=null)cause=cause.getCause();
      assertFalse(cause.getMessage().contains(password));
    });
    runner(PASSWORD).withPropertyValues("staff.username=").run(ctx->assertNotNull(ctx.getStartupFailure()));
  }
}
