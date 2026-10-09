package com.kku.queuenotify;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.service.impl.InMemoryPushSubscriptionService;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PushDemoServiceTest {
  final WebPushSender sender=mock(WebPushSender.class);
  final AtomicReference<Instant> now=new AtomicReference<>(Instant.parse("2026-10-10T00:00:00Z"));
  final Clock clock=new Clock(){public java.time.ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now.get();}};
  InMemoryPushSubscriptionService service(){return new InMemoryPushSubscriptionService(new SubscriptionValidator(),sender,clock);}
  PushSubscriptionRequest request(){return new PushSubscriptionRequest("https://fcm.googleapis.com/wp/demo-test",
      new PushSubscriptionRequest.Keys(PushConfigurationTest.KEY,OrderSubscriptionServiceTest.AUTH));}
  @Test void onlyRegisteredSessionCanSendAndRemoveDoesNotTouchOtherSession(){
    var service=service();var request=request();service.register("one",request);service.register("two",request);
    when(sender.sendTest(request)).thenReturn(201);service.remove("one");
    assertEquals(HttpStatus.NOT_FOUND,assertThrows(PushDemoException.class,()->service.send("one")).getStatus());
    assertEquals(201,service.send("two"));verify(sender,times(1)).sendTest(request);
  }
  @Test void expiredEntriesCannotSend(){var service=service();service.register("one",request());now.set(now.get().plusSeconds(900));
    assertEquals(HttpStatus.NOT_FOUND,assertThrows(PushDemoException.class,()->service.send("one")).getStatus());verifyNoInteractions(sender);}
  @Test void invalidSubscriptionIsRejectedBeforeSending(){var service=service();assertThrows(com.kku.queuenotify.exception.ApiException.class,
      ()->service.register("one",new PushSubscriptionRequest("http://localhost/test",request().keys())));verifyNoInteractions(sender);}
  @Test void capacityIsBoundedAndExpiredEntriesFreeCapacity(){var service=service();for(int i=0;i<100;i++)service.register("session"+i,request());
    assertEquals(HttpStatus.TOO_MANY_REQUESTS,assertThrows(PushDemoException.class,()->service.register("extra",request())).getStatus());
    now.set(now.get().plusSeconds(900));assertDoesNotThrow(()->service.register("extra",request()));}
  @Test void duplicateSendAndReplacementDuringSendAreRejected(){var service=service();var request=request();service.register("one",request);
    when(sender.sendTest(request)).thenAnswer(call->{
      assertEquals(HttpStatus.CONFLICT,assertThrows(PushDemoException.class,()->service.send("one")).getStatus());
      assertEquals(HttpStatus.CONFLICT,assertThrows(PushDemoException.class,()->service.register("one",request)).getStatus());
      assertEquals(HttpStatus.CONFLICT,assertThrows(PushDemoException.class,()->service.remove("one")).getStatus());return 202;});
    assertEquals(202,service.send("one"));verify(sender,times(1)).sendTest(request);}
  @Test void errorsAreSafeAndBusyGuardIsReleased(){var service=service();var request=request();service.register("one",request);
    when(sender.sendTest(request)).thenThrow(new IllegalStateException("TEST_SECRET_ENDPOINT")).thenReturn(201);
    var error=assertThrows(PushDemoException.class,()->service.send("one"));assertEquals(HttpStatus.BAD_GATEWAY,error.getStatus());
    assertFalse(error.getMessage().contains("SECRET"));assertEquals(201,service.send("one"));}
  @Test void missingConfigurationKeepsSafe503(){var service=service();var request=request();service.register("one",request);
    when(sender.sendTest(request)).thenThrow(new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,"Web Push is not configured"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(PushDemoException.class,()->service.send("one")).getStatus());}
}
