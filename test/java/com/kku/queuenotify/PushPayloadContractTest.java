package com.kku.queuenotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.service.NotificationDeliveryService;
import com.kku.queuenotify.service.NotificationStrategy;
import com.kku.queuenotify.service.WebPushSender;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PushPayloadContractTest {
  @Test void readyPayloadSerializesOnlyBrowserFieldsAndLocalQueueDestination() throws Exception {
    PushPayload payload=PushPayload.ready(8L,"คิว A008 พร้อมรับอาหาร");
    var json=new ObjectMapper().readTree(new ObjectMapper().writeValueAsBytes(payload));
    Set<String> names=new HashSet<>();json.fieldNames().forEachRemaining(names::add);
    assertEquals(Set.of("title","body","tag","url"),names);
    assertEquals("อาหารพร้อมแล้ว",json.get("title").asText());
    assertEquals("คิว A008 พร้อมรับอาหาร",json.get("body").asText());
    assertEquals("queue-8-ready",json.get("tag").asText());
    assertEquals("/queue/8",json.get("url").asText());
  }
  @Test void disallowsTokenQueriesFragmentsAndExternalOrAmbiguousDestinations() {
    for(String url:new String[] {"/queue/8?token=owner-fixture", "/queue/8#owner-fixture",
        "https://example.com/queue/8", "//example.com/queue/8", "javascript:alert(1)",
        "/queue/../8", "/queue/%38", "/queue/0", "/queue/-8", "/queue/8/extra",
        "/push-demo.html?token=owner-fixture", "", " /queue/8", "/queue/8\\token", null}) {
      assertThrows(IllegalArgumentException.class,()->new PushPayload("Ready","Body","tag",url));
    }
  }
  @Test void readyFactoryRejectsMissingAndNonpositiveIds() {
    for(Long id:new Long[]{null,0L,-1L}) assertThrows(IllegalArgumentException.class,()->PushPayload.ready(id,"Body"));
    assertEquals("/queue/"+Long.MAX_VALUE,PushPayload.ready(Long.MAX_VALUE,"Body").url());
  }
  @Test void textAndTagAreRequiredAndDemoDestinationIsSupported() {
    assertThrows(IllegalArgumentException.class,()->new PushPayload(null,"Body","tag","/queue/8"));
    assertThrows(IllegalArgumentException.class,()->new PushPayload(" ","Body","tag","/queue/8"));
    assertThrows(IllegalArgumentException.class,()->new PushPayload("Ready",null,"tag","/queue/8"));
    assertThrows(IllegalArgumentException.class,()->new PushPayload("Ready","Body"," ","/queue/8"));
    assertEquals("/push-demo.html",new PushPayload("Test","Body","demo","/push-demo.html").url());
  }
  @Test void contractsExposeSendingAndIdentifierOnlyDeliveryWithoutNetworkCalls() {
    PushPayload payload=PushPayload.ready(8L,"Body");
    WebPushSender sender=new WebPushSender() {
      public int sendTest(com.kku.queuenotify.dto.request.PushSubscriptionRequest subscription) { return 202; }
      public int send(com.kku.queuenotify.dto.request.PushSubscriptionRequest subscription,PushPayload body) {
        assertSame(payload,body); return 201;
      }
    };
    NotificationStrategy strategy=sender::send;
    assertEquals(201,strategy.send(null,payload));assertEquals(202,sender.sendTest(null));
    java.util.concurrent.atomic.AtomicReference<Long> seen=new java.util.concurrent.atomic.AtomicReference<>();
    NotificationDeliveryService delivery=seen::set;delivery.deliverReady(8L);assertEquals(8L,seen.get());
  }
}
