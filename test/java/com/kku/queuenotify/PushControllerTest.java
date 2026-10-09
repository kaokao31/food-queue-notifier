package com.kku.queuenotify;

import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.controller.api.PushController;
import com.kku.queuenotify.exception.PushConfigurationExceptionHandler;
import com.kku.queuenotify.service.impl.PushConfigurationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PushControllerTest {
  private MockMvc mvc(WebPushProperties properties) {
    return MockMvcBuilders.standaloneSetup(
        new PushController(new PushConfigurationServiceImpl(properties)))
        .setControllerAdvice(new PushConfigurationExceptionHandler()).build();
  }

  @Test void servesOnlyPublicKeyWithNoStore() throws Exception {
    WebPushProperties properties = new WebPushProperties();
    properties.setPublicKey(PushConfigurationTest.KEY);
    properties.setPrivateKey("fixture-private-value");
    properties.setSubject("mailto:fixture@example.invalid");
    mvc(properties).perform(get("/api/v1/push/public-key"))
        .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.publicKey").value(PushConfigurationTest.KEY))
        .andExpect(jsonPath("$.privateKey").doesNotExist())
        .andExpect(jsonPath("$.subject").doesNotExist())
        .andExpect(content().string(not(containsString("fixture-private-value"))));
  }

  @Test void missingOrInvalidConfigurationReturnsSafe503Json() throws Exception {
    WebPushProperties properties = new WebPushProperties();
    properties.setPrivateKey("fixture-private-value");
    for (String publicKey : new String[] {"", "invalid-secret-like-value"}) {
      properties.setPublicKey(publicKey);
      mvc(properties).perform(get("/api/v1/push/public-key"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.status").value(503))
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.stackTrace").doesNotExist())
          .andExpect(content().string(not(containsString("fixture-private-value"))))
          .andExpect(content().string(not(containsString("invalid-secret-like-value"))));
    }
  }
}
