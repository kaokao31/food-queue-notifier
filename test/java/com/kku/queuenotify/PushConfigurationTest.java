package com.kku.queuenotify;

import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.impl.PushConfigurationServiceImpl;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class PushConfigurationTest {
  // Public P-256 base point, used as a test fixture; no private credential is included.
  static final String KEY = Base64.getUrlEncoder().withoutPadding().encodeToString(
      HexFormat.of().parseHex("04"
          + "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296"
          + "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5"));

  @Test void missingPublicKeyIsUnavailableWithoutRequiringPrivateKey() {
    WebPushProperties properties = new WebPushProperties();
    for (String value : new String[] {null, "", "  "}) {
      properties.setPublicKey(value);
      PushDemoException ex = assertThrows(PushDemoException.class,
          () -> new PushConfigurationServiceImpl(properties).getPublicKey());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }
  }

  @Test void returnsCanonicalPublicKeyWithOptionalPadding() {
    WebPushProperties properties = new WebPushProperties();
    properties.setPublicKey(" " + KEY + "= ");
    properties.setPrivateKey("fixture-private-value");
    assertEquals(KEY, new PushConfigurationServiceImpl(properties).getPublicKey());
  }

  @Test void rejectsBadEncodingLengthAndOffCurvePointWithoutEchoingInput() {
    WebPushProperties properties = new WebPushProperties();
    byte[] offCurve = new byte[65]; offCurve[0] = 4;
    byte[] compressed = new byte[65]; compressed[0] = 2;
    for (String value : new String[] {"invalid-secret-like-value", "!".repeat(87),
        Base64.getUrlEncoder().withoutPadding().encodeToString(offCurve),
        Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)}) {
      properties.setPublicKey(value);
      PushDemoException ex = assertThrows(PushDemoException.class,
          () -> new PushConfigurationServiceImpl(properties).getPublicKey());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
      assertEquals("Web Push public key configuration is invalid", ex.getMessage());
      assertFalse(ex.getMessage().contains(value));
    }
  }
}
