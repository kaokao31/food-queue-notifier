package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import java.util.Base64;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionValidator {
  /** Validate browser encryption keys only; endpoint authorization is a separate step. */
  public void validateKeys(PushSubscriptionRequest request) {
    try {
      if (request == null || request.keys() == null) throw new IllegalArgumentException();
      byte[] publicKey = decode(request.keys().p256dh(), 65);
      decode(request.keys().auth(), 16);
      if (publicKey[0] != 4) throw new IllegalArgumentException();
      ECPoint point = SECNamedCurves.getByName("secp256r1").getCurve().decodePoint(publicKey);
      if (point.isInfinity() || !point.isValid()) throw new IllegalArgumentException();
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Subscription keys are invalid");
    }
  }

  private byte[] decode(String value, int expectedLength) {
    if (value == null || value.length() > 256 || !value.matches("[A-Za-z0-9_-]+={0,2}")) {
      throw new IllegalArgumentException();
    }
    byte[] bytes = Base64.getUrlDecoder().decode(value);
    String canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    if (bytes.length != expectedLength || !canonical.equals(value.replaceFirst("=+$", ""))) {
      throw new IllegalArgumentException();
    }
    return bytes;
  }
}
