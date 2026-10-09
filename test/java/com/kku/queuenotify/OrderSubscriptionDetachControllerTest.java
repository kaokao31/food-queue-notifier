package com.kku.queuenotify;

import com.kku.queuenotify.controller.api.OrderSubscriptionController;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.exception.PushConfigurationExceptionHandler;
import com.kku.queuenotify.service.OrderSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderSubscriptionDetachControllerTest {
  private MockMvc mvc(OrderSubscriptionService service) {
    return MockMvcBuilders.standaloneSetup(new OrderSubscriptionController(service))
        .setControllerAdvice(new PushConfigurationExceptionHandler()).build();
  }
  @Test void forwardsOwnerTokenAndReturns204WithoutBody() throws Exception {
    OrderSubscriptionService service=mock(OrderSubscriptionService.class);
    MockMvc mvc=mvc(service);
    for(int i=0;i<2;i++) mvc.perform(delete("/api/v1/orders/8/subscription")
        .header("X-Queue-Token","owner-fixture")).andExpect(status().isNoContent())
        .andExpect(content().string(""));
    verify(service,times(2)).detach(8L,"owner-fixture"); verifyNoMoreInteractions(service);
  }
  @Test void missingAccessReturnsSafe503WithoutCaching() throws Exception {
    OrderSubscriptionService service=mock(OrderSubscriptionService.class);
    doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Order access service is not available"))
        .when(service).detach(8L,null);
    mvc(service).perform(delete("/api/v1/orders/8/subscription"))
        .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control","no-store"))
        .andExpect(jsonPath("$.status").value(503));
    verify(service).detach(8L,null);
  }
  @Test void deniedTokenAndMissingOrderKeepTheirErrorStatuses() throws Exception {
    OrderSubscriptionService service=mock(OrderSubscriptionService.class);
    doThrow(new ApiException(HttpStatus.FORBIDDEN,"denied")).when(service).detach(8L,"wrong-fixture");
    doThrow(new ApiException(HttpStatus.NOT_FOUND,"Order not found")).when(service).detach(99L,"owner-fixture");
    MockMvc mvc=mvc(service);
    mvc.perform(delete("/api/v1/orders/8/subscription").header("X-Queue-Token","wrong-fixture"))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/orders/99/subscription").header("X-Queue-Token","owner-fixture"))
        .andExpect(status().isNotFound());
  }
}
