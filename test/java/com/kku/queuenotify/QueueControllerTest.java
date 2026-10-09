package com.kku.queuenotify;

import com.kku.queuenotify.config.SecurityConfig;
import com.kku.queuenotify.controller.api.QueueController;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QueueControllerTest {
  // Route/filter contract tests; the separate PostgreSQL suite uses the real QueueService.
  @TestConfiguration(proxyBeanMethods=false) @EnableWebMvc @Import(QueueController.class)
  static class Web { @Bean QueueService queues(){return mock(QueueService.class);} }
  interface Check {void run(MockMvc mvc,QueueService service) throws Exception;}
  void check(Check check){new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class))
      .withUserConfiguration(SecurityConfig.class,Web.class)
      .run(ctx->{assertNull(ctx.getStartupFailure());check.run(MockMvcBuilders.webAppContextSetup(ctx)
          .apply(springSecurity()).build(),ctx.getBean(QueueService.class));});}
  QueueResponse response(QueueStatus status){return new QueueResponse(8L,3,null,status,null);}
  @Test void readForwardsOwnerHeaderAndReturnsCredentialFreeNoStoreDto(){check((mvc,service)->{
    when(service.get(8L,"test-owner")).thenReturn(response(QueueStatus.WAITING));
    mvc.perform(get("/api/v1/queues/8").header("X-Queue-Token","test-owner"))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
        .andExpect(jsonPath("$.id").value(8)).andExpect(jsonPath("$.status").value("WAITING"))
        .andExpect(jsonPath("$.tokenHash").doesNotExist()).andExpect(jsonPath("$.queueToken").doesNotExist());
    verify(service).get(8L,"test-owner");
  });}
  @Test void missingAndWrongOwnerTokensReachServiceAndReturn403(){check((mvc,service)->{
    when(service.get(8L,null)).thenThrow(new ApiException(HttpStatus.FORBIDDEN,"Order access denied"));
    when(service.get(8L,"wrong")).thenThrow(new ApiException(HttpStatus.FORBIDDEN,"Order access denied"));
    mvc.perform(get("/api/v1/queues/8")).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/queues/8").header("X-Queue-Token","wrong"))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Order access denied"))
        .andExpect(header().string("Cache-Control","no-store"));
  });}
  @Test void anonymousAndNonStaffAdvanceAreBlockedBeforeService(){check((mvc,service)->{
    mvc.perform(patch("/api/v1/queues/8/advance").with(csrf())).andExpect(status().isUnauthorized());
    mvc.perform(patch("/api/v1/queues/8/advance").with(user("customer").roles("CUSTOMER")).with(csrf()))
        .andExpect(status().isForbidden());verifyNoInteractions(service);
  });}
  @Test void staffAdvanceRequiresCsrfAndReturnsNextState(){check((mvc,service)->{
    when(service.advance(8L)).thenReturn(response(QueueStatus.PREPARING));
    mvc.perform(patch("/api/v1/queues/8/advance").with(user("staff").roles("STAFF")))
        .andExpect(status().isForbidden());verifyNoInteractions(service);
    mvc.perform(patch("/api/v1/queues/8/advance").with(user("staff").roles("STAFF")).with(csrf()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PREPARING"));verify(service).advance(8L);
  });}
  @Test void customerCancelNeedsCsrfAndForwardsOwnerToken(){check((mvc,service)->{
    when(service.cancel(8L,"owner")).thenReturn(response(QueueStatus.CANCELLED));
    mvc.perform(patch("/api/v1/queues/8/cancel").header("X-Queue-Token","owner"))
        .andExpect(status().isForbidden());verifyNoInteractions(service);
    mvc.perform(patch("/api/v1/queues/8/cancel").header("X-Queue-Token","owner").with(csrf()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));verify(service).cancel(8L,"owner");
  });}
  @Test void absentQueueAndInvalidTransitionPreserveHttpStatus(){check((mvc,service)->{
    when(service.get(99L,"owner")).thenThrow(new ApiException(HttpStatus.NOT_FOUND,"Queue not found"));
    when(service.cancel(8L,"owner")).thenThrow(new ApiException(HttpStatus.CONFLICT,"Cannot cancel ready queue"));
    mvc.perform(get("/api/v1/queues/99").header("X-Queue-Token","owner")).andExpect(status().isNotFound());
    mvc.perform(patch("/api/v1/queues/8/cancel").header("X-Queue-Token","owner").with(csrf()))
        .andExpect(status().isConflict()).andExpect(header().string("Cache-Control","no-store"));
  });}
  @Test void nonNumericIdIs400WithoutCallingService(){check((mvc,service)->{
    mvc.perform(get("/api/v1/queues/invalid")).andExpect(status().isBadRequest());verifyNoInteractions(service);
  });}
}
