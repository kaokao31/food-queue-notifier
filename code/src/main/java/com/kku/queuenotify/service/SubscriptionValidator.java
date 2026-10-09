package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import java.util.Base64;
import java.net.URI;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionValidator {
  /** Complete validation used before storing or sending a subscription. */
  public void validate(PushSubscriptionRequest request) {
    if (request == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Subscription is required");
    }
    validateEndpoint(request.endpoint());
    validateKeys(request);
  }

  /** This project supports Android Chrome / FCM endpoints only. No network lookup is performed. */
  public void validateEndpoint(String endpoint) {
    try {
      if (endpoint == null || endpoint.isBlank() || endpoint.length() > 2048) {
        throw new IllegalArgumentException();
      }
      URI uri = URI.create(endpoint);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || !"fcm.googleapis.com".equalsIgnoreCase(uri.getHost())
          || uri.getRawUserInfo() != null
          || (uri.getPort() != -1 && uri.getPort() != 443)
          || uri.getRawQuery() != null
          || uri.getRawFragment() != null
          || uri.getRawPath() == null
          || !uri.getRawPath().matches("/(?:fcm/send|wp)/[A-Za-z0-9_:-]+")) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException ex) {
      // Endpoint URLs can contain subscription credentials; do not echo them.
      throw new ApiException(HttpStatus.BAD_REQUEST,
          "Subscription endpoint is invalid; supported provider: Android Chrome/FCM");
    }
  }


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
