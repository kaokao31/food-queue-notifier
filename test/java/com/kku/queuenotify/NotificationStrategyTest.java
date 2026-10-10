package com.kku.queuenotify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.NotificationStrategy;
import com.kku.queuenotify.service.WebPushSender;
import com.kku.queuenotify.service.impl.ConsoleNotificationStrategy;
import com.kku.queuenotify.service.impl.PushNotificationStrategy;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class NotificationStrategyTest {
  private ApplicationContextRunner context() {
    return new ApplicationContextRunner().withUserConfiguration(
        ConsoleNotificationStrategy.class, PushNotificationStrategy.class);
  }
  private PushPayload payload() { return PushPayload.ready(8L,"fixture-body-secret"); }
  private PushSubscriptionRequest subscription() {
    return new PushSubscriptionRequest("https://fcm.googleapis.com/wp/endpoint-secret",
        new PushSubscriptionRequest.Keys("public-key-fixture","auth-secret-fixture"));
  }
  private static class FixtureSender implements WebPushSender {
    int calls; int status=201; PushSubscriptionRequest seenSubscription; PushPayload seenPayload;
    RuntimeException error;
    public int sendTest(PushSubscriptionRequest request) { throw new AssertionError("test send must not be called"); }
    public int send(PushSubscriptionRequest request,PushPayload payload) {
      calls++;seenSubscription=request;seenPayload=payload;if(error!=null)throw error;return status;
    }
  }
  @Test void missingModeSelectsConsoleWithoutNeedingSender() {
    context().run(ctx->{
      assertNull(ctx.getStartupFailure());assertEquals(1,ctx.getBeansOfType(NotificationStrategy.class).size());
      assertInstanceOf(ConsoleNotificationStrategy.class,ctx.getBean(NotificationStrategy.class));
      assertEquals(0,ctx.getBean(NotificationStrategy.class).send(subscription(),payload()));
    });
  }
  @Test void explicitConsoleNeverCallsAvailableSenderAndReturnsPreviewZero() {
    FixtureSender sender=new FixtureSender();
    context().withBean(WebPushSender.class,()->sender).withPropertyValues("notification.mode=console").run(ctx->{
      assertEquals(1,ctx.getBeansOfType(NotificationStrategy.class).size());
      assertEquals(0,ctx.getBean(NotificationStrategy.class).send(subscription(),payload()));assertEquals(0,sender.calls);
    });
  }
  @Test void webpushDelegatesSameObjectsOnceAndPreservesProviderStatus() {
    FixtureSender sender=new FixtureSender();sender.status=410;
    context().withBean(WebPushSender.class,()->sender).withPropertyValues("notification.mode=webpush").run(ctx->{
      assertNull(ctx.getStartupFailure());assertEquals(1,ctx.getBeansOfType(NotificationStrategy.class).size());
      var strategy=ctx.getBean(NotificationStrategy.class);assertInstanceOf(PushNotificationStrategy.class,strategy);
      var request=subscription();var payload=payload();assertEquals(410,strategy.send(request,payload));
      assertSame(request,sender.seenSubscription);assertSame(payload,sender.seenPayload);assertEquals(1,sender.calls);
    });
  }
  @Test void webpushPropagatesFailureWithoutConsoleFallbackOrSuccess() {
    FixtureSender sender=new FixtureSender();sender.error=new PushDemoException(HttpStatus.BAD_GATEWAY,"Web Push sending failed");
    context().withBean(WebPushSender.class,()->sender).withPropertyValues("notification.mode=webpush").run(ctx->{
      assertSame(sender.error,assertThrows(PushDemoException.class,()->ctx.getBean(NotificationStrategy.class).send(subscription(),payload())));
      assertEquals(1,sender.calls);
    });
  }
  @Test void unsupportedModeSelectsNoStrategyRatherThanPretendingDelivery() {
    for(String mode:new String[]{"line","unknown"}) context().withPropertyValues("notification.mode="+mode).run(ctx->{
      assertTrue(ctx.getBeansOfType(NotificationStrategy.class).isEmpty());
    });
  }
  @Test void consoleLogDoesNotContainEndpointKeysOrPayloadText() {
    Logger logger=(Logger)LoggerFactory.getLogger(ConsoleNotificationStrategy.class);
    ListAppender<ILoggingEvent> capture=new ListAppender<>();capture.start();logger.addAppender(capture);
    try {
      assertEquals(0,new ConsoleNotificationStrategy().send(subscription(),payload()));
      assertEquals(1,capture.list.size());
      assertEquals("Local notification preview (no provider request)",capture.list.get(0).getFormattedMessage());
      assertFalse(capture.list.get(0).getFormattedMessage().contains("secret"));
    } finally { logger.detachAppender(capture);capture.stop(); }
  }
}
