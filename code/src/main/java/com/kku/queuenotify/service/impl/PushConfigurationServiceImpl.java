package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.PushConfigurationService;
import java.util.Base64;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PushConfigurationServiceImpl implements PushConfigurationService {
  private final WebPushProperties properties;

  public PushConfigurationServiceImpl(WebPushProperties properties) {
    this.properties = properties;
  }

  @Override
  public String getPublicKey() {
    String key = properties.getPublicKey();
    if (key == null || key.isBlank()) {
      throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,
          "Web Push public key is not configured");
    }
    try {
      String value = key.trim();
      if (!value.matches("[A-Za-z0-9_-]{87}=?")) {
        throw new IllegalArgumentException();
      }
      byte[] bytes = Base64.getUrlDecoder().decode(value);
      if (bytes.length != 65 || bytes[0] != 4) {
        throw new IllegalArgumentException();
      }
      ECPoint point = SECNamedCurves.getByName("secp256r1").getCurve().decodePoint(bytes);
      if (point.isInfinity() || !point.isValid()) {
        throw new IllegalArgumentException();
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } catch (IllegalArgumentException ex) {
      // Do not include configured key values or decoder error details in the response.
      throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,
          "Web Push public key configuration is invalid");
    }
  }
}
