package com.kku.queuenotify;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.SubscriptionValidator;
import jakarta.validation.Validation;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionKeysTest {
  private final SubscriptionValidator validator = new SubscriptionValidator();
  private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/test-fixture";
  private static final String AUTH = Base64.getUrlEncoder().withoutPadding()
      .encodeToString(new byte[16]); // Only a test fixture, never a production auth secret.

  private PushSubscriptionRequest request(String publicKey, String auth) {
    return new PushSubscriptionRequest(ENDPOINT, new PushSubscriptionRequest.Keys(publicKey, auth));
  }
  private void rejects(PushSubscriptionRequest request) {
    ApiException ex = assertThrows(ApiException.class, () -> validator.validateKeys(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals("Subscription keys are invalid", ex.getMessage());
  }

  @Test void acceptsBrowserKeysWithAndWithoutPadding() {
    assertDoesNotThrow(() -> validator.validateKeys(request(PushConfigurationTest.KEY, AUTH)));
    assertDoesNotThrow(() -> validator.validateKeys(request(PushConfigurationTest.KEY + "=", AUTH + "==")));
  }

  @Test void rejectsMissingKeysAndNullRequest() {
    rejects(null);
    rejects(new PushSubscriptionRequest(ENDPOINT, null));
    rejects(request(null, AUTH));
    rejects(request(PushConfigurationTest.KEY, null));
    rejects(request("", AUTH));
    rejects(request(PushConfigurationTest.KEY, ""));
  }

  @Test void rejectsMalformedEncodingAndWrongLengthsWithoutEchoingSecrets() {
    rejects(request("+".repeat(87), AUTH));
    rejects(request("x".repeat(257), AUTH));
    rejects(request(PushConfigurationTest.KEY, "fixture-secret-value"));
    rejects(request(PushConfigurationTest.KEY, " " + AUTH));
    rejects(request(PushConfigurationTest.KEY, AUTH + "==="));
    for (int length : new int[] {15, 17}) {
      rejects(request(PushConfigurationTest.KEY,
          Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[length])));
    }
    rejects(request(Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[64]), AUTH));
  }

  @Test void rejectsOffCurvePointAndWrongPointEncoding() {
    byte[] point = new byte[65]; point[0] = 4;
    rejects(request(Base64.getUrlEncoder().withoutPadding().encodeToString(point), AUTH));
    point[0] = 2;
    rejects(request(Base64.getUrlEncoder().withoutPadding().encodeToString(point), AUTH));
  }

  @Test void nestedBeanValidationRejectsIncompleteRequest() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var beanValidator = factory.getValidator();
      assertTrue(beanValidator.validate(request(PushConfigurationTest.KEY, AUTH)).isEmpty());
      assertFalse(beanValidator.validate(new PushSubscriptionRequest("http://example.invalid", null)).isEmpty());
      assertFalse(beanValidator.validate(request("", "")).isEmpty());
      assertFalse(beanValidator.validate(new PushSubscriptionRequest("https://" + "x".repeat(2048),
          new PushSubscriptionRequest.Keys(PushConfigurationTest.KEY, AUTH))).isEmpty());
    }
  }

  @Test void objectLoggingDoesNotExposeEndpointOrAuthSecret() {
    PushSubscriptionRequest request = request(PushConfigurationTest.KEY, "fixture-secret-value");
    assertFalse(request.toString().contains(ENDPOINT));
    assertFalse(request.toString().contains("fixture-secret-value"));
    assertFalse(request.keys().toString().contains("fixture-secret-value"));
  }
}
