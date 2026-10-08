package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import java.net.URI;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionValidator {
  public void validate(PushSubscriptionRequest r) {
    try {
      var u = URI.create(r.endpoint());
      var k = Base64.getUrlDecoder().decode(r.keys().p256dh());
      var a = Base64.getUrlDecoder().decode(r.keys().auth());
      if (!"https".equalsIgnoreCase(u.getScheme())
          || !"fcm.googleapis.com".equalsIgnoreCase(u.getHost())
          || u.getRawUserInfo() != null
          || (u.getPort() != -1 && u.getPort() != 443)
          || u.getRawQuery() != null
          || u.getRawFragment() != null
          || !(u.getPath().startsWith("/fcm/send/") || u.getPath().startsWith("/wp/"))
          || k.length != 65
          || k[0] != 4
          || a.length != 16) throw new IllegalArgumentException();
    } catch (Exception ex) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Subscription ไม่ถูกต้อง รองรับ Android Chrome/FCM");
    }
  }
}
