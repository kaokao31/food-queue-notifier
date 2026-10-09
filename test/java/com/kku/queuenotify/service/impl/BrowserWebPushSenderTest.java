package com.kku.queuenotify.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.SubscriptionValidator;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class BrowserWebPushSenderTest {
  // Synthetic scalar 1 and base point are test-only fixtures, not deployment credentials.
  private WebPushProperties settings() {
    var p=new WebPushProperties();byte[] scalar=new byte[32];scalar[31]=1;
    p.setPrivateKey(Base64.getUrlEncoder().withoutPadding().encodeToString(scalar));
    p.setPublicKey(Base64.getUrlEncoder().withoutPadding().encodeToString(
        SECNamedCurves.getByName("secp256r1").getG().getEncoded(false)));
    p.setSubject("mailto:test@example.invalid");return p;
  }
  private PushSubscriptionRequest subscription() {
    return new PushSubscriptionRequest("https://fcm.googleapis.com/wp/test-fixture",
        new PushSubscriptionRequest.Keys(settings().getPublicKey(),
            Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16])));
  }
  private BrowserWebPushSender sender(WebPushProperties p,BrowserWebPushSender.Transport transport) {
    return new BrowserWebPushSender(p,new SubscriptionValidator(),new ObjectMapper(),transport);
  }
  private PushPayload payload() { return PushPayload.ready(8L,"คิวพร้อมแล้ว"); }
  @Test void missingConfigurationFails503BeforeTransport() {
    AtomicInteger calls=new AtomicInteger();var sender=sender(new WebPushProperties(),r->{calls.incrementAndGet();return 201;});
    var ex=assertThrows(PushDemoException.class,()->sender.send(subscription(),payload()));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE,ex.getStatus());assertEquals(0,calls.get());
  }
  @Test void mismatchedZeroAndMalformedPrivateKeysAreRejectedWithoutLeakingSettings() {
    for(String key:new String[]{"secret-fixture",Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]),
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI"}) {
      var p=settings();p.setPrivateKey(key);
      var ex=assertThrows(PushDemoException.class,()->sender(p,r->{fail("must not send");return 201;}).send(subscription(),payload()));
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE,ex.getStatus());assertFalse(ex.getMessage().contains(key));assertNull(ex.getCause());
    }
  }
  @Test void invalidSubjectIsRejectedBeforeSending() {
    for(String subject:new String[]{"", "http://example.com", "mailto:invalid", "https://user:secret@example.com", "mailto:a@b#secret"}) {
      var p=settings();p.setSubject(subject);
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(PushDemoException.class,
          ()->sender(p,r->{fail("must not send");return 201;}).send(subscription(),payload())).getStatus());
    }
  }
  @Test void invalidEndpointOrEncryptionKeysNeverReachTransport() {
    var sender=sender(settings(),r->{fail("must not send");return 201;});
    for(var request:new PushSubscriptionRequest[]{new PushSubscriptionRequest("https://localhost/wp/test",subscription().keys()),
        new PushSubscriptionRequest(subscription().endpoint(),new PushSubscriptionRequest.Keys("bad","bad"))}) {
      assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException.class,()->sender.send(request,payload())).getStatus());
    }
  }
  @Test void encryptsAndSignsRequestWithoutCallingRealProvider() throws Exception {
    var sender=sender(settings(),request->{
      assertEquals(subscription().endpoint(),request.getURI().toString());
      assertEquals("aes128gcm",request.getFirstHeader("Content-Encoding").getValue());
      assertEquals("60",request.getFirstHeader("TTL").getValue());
      assertNotNull(request.getFirstHeader("Authorization"));
      byte[] encrypted=EntityUtils.toByteArray(request.getEntity());
      assertTrue(encrypted.length>86);
      assertFalse(new String(encrypted,StandardCharsets.UTF_8).contains("queue-8-ready"));
      assertFalse(java.util.Arrays.equals(encrypted,new ObjectMapper().writeValueAsBytes(payload())));
      return 201;
    });
    assertEquals(201,sender.send(subscription(),payload()));
  }
  @Test void returnsProviderErrorStatusesWithoutClaimingDeliveryOrRetrying() {
    for(int code:new int[]{202,404,410,429,500}) {
      AtomicInteger calls=new AtomicInteger();
      assertEquals(code,sender(settings(),r->{calls.incrementAndGet();return code;}).send(subscription(),payload()));
      assertEquals(1,calls.get());
    }
  }
  @Test void timeoutAndOtherFailuresReturnSafe502WithoutExceptionDetails() {
    for(Exception cause:new Exception[]{new TimeoutException("secret-fixture"),new java.io.IOException("endpoint-private-key-fixture")}) {
      var ex=assertThrows(PushDemoException.class,()->sender(settings(),r->{throw cause;}).send(subscription(),payload()));
      assertEquals(HttpStatus.BAD_GATEWAY,ex.getStatus());assertEquals("Web Push sending failed",ex.getMessage());assertNull(ex.getCause());
    }
  }
  @Test void restoresInterruptedFlagAndSupportsDemoPayload() {
    try {
      var ex=assertThrows(PushDemoException.class,()->sender(settings(),r->{throw new InterruptedException();}).send(subscription(),payload()));
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE,ex.getStatus());assertTrue(Thread.currentThread().isInterrupted());
    } finally { Thread.interrupted(); }
    assertEquals(201,sender(settings(),r->201).sendTest(subscription()));
  }
}
