package com.kku.queuenotify;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.SubscriptionValidator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionEndpointTest {
  private final SubscriptionValidator validator = new SubscriptionValidator();
  private static final String VALID = "https://fcm.googleapis.com/fcm/send/token:fixture_123-xyz";
  private PushSubscriptionRequest request(String endpoint) {
    return new PushSubscriptionRequest(endpoint, new PushSubscriptionRequest.Keys(
        PushConfigurationTest.KEY, Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16])));
  }
  private void rejects(String endpoint) {
    ApiException ex = assertThrows(ApiException.class, () -> validator.validateEndpoint(endpoint));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals("Subscription endpoint is invalid; supported provider: Android Chrome/FCM", ex.getMessage());
    if (endpoint != null && !endpoint.isBlank()) assertFalse(ex.getMessage().contains(endpoint));
  }

  @Test void acceptsSupportedPathsAndDefaultOrExplicitHttpsPort() {
    for (String endpoint : new String[] {VALID, "https://fcm.googleapis.com/wp/token-fixture",
        "https://FCM.GOOGLEAPIS.COM:443/fcm/send/token_fixture"}) {
      assertDoesNotThrow(() -> validator.validate(request(endpoint)));
    }
  }

  @Test void rejectsUntrustedSchemeHostAndPrivateAddresses() {
    for (String endpoint : new String[] {"http://fcm.googleapis.com/fcm/send/token",
        "https://example.invalid/fcm/send/token", "https://fcm.googleapis.com.evil.invalid/wp/token",
        "https://fcm.googleapis.com./wp/token", "https://127.0.0.1/wp/token",
        "https://10.0.0.1/wp/token", "https://[::1]/wp/token", "https://localhost/wp/token",
        "https://fcm%2egoogleapis.com/wp/token"}) rejects(endpoint);
  }

  @Test void rejectsUserInfoQueryFragmentAndUnexpectedPorts() {
    for (String endpoint : new String[] {"https://user@fcm.googleapis.com/wp/token",
        "https://fcm.googleapis.com@evil.invalid/wp/token", VALID + "?secret=fixture",
        VALID + "#secret", "https://fcm.googleapis.com:444/wp/token",
        "https://fcm.googleapis.com:80/wp/token"}) rejects(endpoint);
  }

  @Test void rejectsMissingTokenUnexpectedPathsAndEncodedPathTricks() {
    for (String endpoint : new String[] {"https://fcm.googleapis.com/",
        "https://fcm.googleapis.com/wp/", "https://fcm.googleapis.com/fcm/send/",
        "https://fcm.googleapis.com/other/token", "https://fcm.googleapis.com/wp/token/extra",
        "https://fcm.googleapis.com/wp/../token", "https://fcm.googleapis.com/wp/%2e%2e",
        "https://fcm.googleapis.com/wp/token%2Fextra", "https://fcm.googleapis.com/wp/%0Atoken"}) rejects(endpoint);
  }

  @Test void rejectsNullBlankMalformedAndOversizedEndpoints() {
    for (String endpoint : new String[] {null, "", " ", ":not-a-uri", VALID + " ",
        "https://fcm.googleapis.com/wp/" + "x".repeat(2048)}) rejects(endpoint);
  }

  @Test void fullValidationAlsoRequiresValidEncryptionKeys() {
    ApiException nullRequest = assertThrows(ApiException.class, () -> validator.validate(null));
    assertEquals(HttpStatus.BAD_REQUEST, nullRequest.getStatus());
    ApiException missingKeys = assertThrows(ApiException.class,
        () -> validator.validate(new PushSubscriptionRequest(VALID, null)));
    assertEquals(HttpStatus.BAD_REQUEST, missingKeys.getStatus());
    assertThrows(ApiException.class, () -> validator.validate(new PushSubscriptionRequest(VALID,
        new PushSubscriptionRequest.Keys(PushConfigurationTest.KEY, "bad-secret-fixture"))));
  }
}
