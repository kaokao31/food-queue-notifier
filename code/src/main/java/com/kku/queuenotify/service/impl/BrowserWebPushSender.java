package com.kku.queuenotify.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.PushConfigurationService;
import com.kku.queuenotify.service.SubscriptionValidator;
import com.kku.queuenotify.service.WebPushSender;
import java.math.BigInteger;
import java.net.URI;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BrowserWebPushSender implements WebPushSender {
  @FunctionalInterface
  interface Transport { int send(HttpPost encryptedRequest) throws Exception; }
  private final WebPushProperties properties;
  private final PushConfigurationService configurationService;
  private final SubscriptionValidator validator;
  private final ObjectMapper json;
  private final Transport transport;

  @Autowired
  public BrowserWebPushSender(WebPushProperties properties, PushConfigurationService configurationService,
      SubscriptionValidator validator, ObjectMapper json) {
    this(properties, configurationService, validator, json, BrowserWebPushSender::sendHttp);
  }
  // Test seam replaces only HTTP transport; validation, encryption and signing still run.
  BrowserWebPushSender(WebPushProperties properties, PushConfigurationService configurationService,
      SubscriptionValidator validator, ObjectMapper json, Transport transport) {
    this.configurationService=configurationService; this.properties=properties; this.validator=validator; this.json=json; this.transport=transport;
  }

  @Override public int sendTest(PushSubscriptionRequest subscription) {
    return send(subscription,new PushPayload("ทดสอบแจ้งเตือนคิวอาหาร",
        "ข้อความนี้เป็นการทดสอบ Web Push","queue-demo","/push-demo.html"));
  }

  @Override public int send(PushSubscriptionRequest subscription, PushPayload payload) {
    String[] settings=validatedSettings();
    validator.validate(subscription);
    if (payload==null) throw new IllegalArgumentException("Notification payload is required");
    try {
      if (Security.getProvider("BC")==null) Security.addProvider(new BouncyCastleProvider());
      var service=new PushService(settings[0],settings[1],settings[2]);
      var notification=Notification.builder().endpoint(subscription.endpoint())
          .userPublicKey(subscription.keys().p256dh()).userAuth(subscription.keys().auth())
          .payload(json.writeValueAsBytes(payload)).ttl(60).urgency(Urgency.HIGH).build();
      HttpPost request=service.preparePost(notification,Encoding.AES128GCM);
      return transport.send(request);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,"Web Push sending was interrupted");
    } catch (Exception ex) {
      // Do not expose keys, endpoint, upstream bodies or the exception cause.
      throw new PushDemoException(HttpStatus.BAD_GATEWAY,"Web Push sending failed");
    }
  }

  private String[] validatedSettings() {
    try {
      String publicKey=configurationService.getPublicKey();
      String privateKey=properties.getPrivateKey();
      String subject=properties.getSubject();
      if (privateKey==null || !privateKey.trim().matches("[A-Za-z0-9_-]{43}=?")
          || subject==null || subject.isBlank()) throw new IllegalArgumentException();
      privateKey=privateKey.trim(); subject=subject.trim();
      byte[] bytes=Base64.getUrlDecoder().decode(privateKey);
      var curve=SECNamedCurves.getByName("secp256r1"); var scalar=new BigInteger(1,bytes);
      if (bytes.length!=32 || scalar.signum()<=0 || scalar.compareTo(curve.getN())>=0
          || !Arrays.equals(curve.getG().multiply(scalar).normalize().getEncoded(false),
              Base64.getUrlDecoder().decode(publicKey))) throw new IllegalArgumentException();
      URI uri=URI.create(subject);
      boolean mail="mailto".equalsIgnoreCase(uri.getScheme()) && uri.isOpaque()
          && uri.getRawSchemeSpecificPart().matches("[^?\\s@]+@[^?\\s@]+") && uri.getRawFragment()==null;
      boolean https="https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null
          && uri.getRawUserInfo()==null && uri.getRawFragment()==null;
      if (!mail && !https) throw new IllegalArgumentException();
      return new String[]{publicKey,Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),subject};
    } catch (Exception ex) {
      throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,"Web Push configuration is missing or invalid");
    }
  }

  private static int sendHttp(HttpPost request) throws Exception {
    RequestConfig config=RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000)
        .setConnectionRequestTimeout(10000).setRedirectsEnabled(false).build();
    try (var client=HttpAsyncClients.custom().setDefaultRequestConfig(config).build()) {
      client.start(); var future=client.execute(request,null);
      try { return future.get(15,TimeUnit.SECONDS).getStatusLine().getStatusCode(); }
      finally { if (!future.isDone()) future.cancel(true); }
    }
  }
}
