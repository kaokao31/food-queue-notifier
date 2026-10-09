package com.kku.queuenotify;

import com.kku.queuenotify.controller.api.OrderSubscriptionController;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.exception.PushConfigurationExceptionHandler;
import com.kku.queuenotify.service.OrderSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderSubscriptionControllerTest {
  private MockMvc mvc(OrderSubscriptionService service) {
    return MockMvcBuilders.standaloneSetup(new OrderSubscriptionController(service))
        .setControllerAdvice(new PushConfigurationExceptionHandler()).build();
  }
  private String body() {
    return "{\"endpoint\":\"https://fcm.googleapis.com/wp/fixture\",\"keys\":{\"p256dh\":\""
        +PushConfigurationTest.KEY+"\",\"auth\":\""+OrderSubscriptionServiceTest.AUTH+"\"}}";
  }
  @Test void delegatesTokenAndReturnsNoContent() throws Exception {
    OrderSubscriptionService service=mock(OrderSubscriptionService.class);
    mvc(service).perform(post("/api/v1/orders/8/subscription").header("X-Queue-Token","owner-fixture")
        .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isNoContent());
    verify(service).attach(eq(8L),eq("owner-fixture"),any());
  }
  @Test void invalidRequestIsRejectedBeforeService() throws Exception {
    OrderSubscriptionService service=mock(OrderSubscriptionService.class);
    mvc(service).perform(post("/api/v1/orders/8/subscription")
        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
  @Test void missingAccessServiceReturnsSafe503Json() throws Exception {
    OrderSubscriptionService service=mock(OrderSubscriptionService.class);
    doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Order access service is not available"))
        .when(service).attach(eq(8L),isNull(),any());
    mvc(service).perform(post("/api/v1/orders/8/subscription")
        .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503)).andExpect(jsonPath("$.message").value("Order access service is not available"));
  }
}
