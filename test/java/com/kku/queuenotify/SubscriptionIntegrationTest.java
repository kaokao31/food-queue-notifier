package com.kku.queuenotify;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubscriptionIntegrationTest extends PushApiTestSupport {
  @Test void registrationRequiresBothCsrfAndCorrectBillToken() throws Exception {
    var first=create();var second=create();var csrf=csrf(null);var body=json.writeValueAsString(subscription());String path="/api/v1/orders/"+first.id()+"/subscription";
    mvc.perform(post(path).header("X-Queue-Token",first.token()).contentType("application/json").content(body)).andExpect(status().isForbidden());
    for(var token:List.of("wrong",second.token()))mvc.perform(post(path).session(csrf.session()).header(csrf.header(),csrf.value())
        .header("X-Queue-Token",token).contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(post(path).session(csrf.session()).header(csrf.header(),csrf.value()).contentType("application/json").content(body)).andExpect(status().isForbidden());
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM push_subscription",Integer.class));attach(first,csrf);
    assertNotNull(jdbc.queryForObject("SELECT push_subscription_id FROM orders WHERE id=?",Long.class,first.id()));verifyNoInteractions(provider);
  }
  @Test void invalidProviderEndpointAndKeysCannotPersistSubscription() throws Exception {
    var order=create();var token=csrf(null);
    for(var request:List.of(new PushSubscriptionRequest("https://localhost/wp/test",subscription().keys()),
        new PushSubscriptionRequest(subscription().endpoint(),new PushSubscriptionRequest.Keys("bad","bad"))))
      mvc.perform(post("/api/v1/orders/"+order.id()+"/subscription").session(token.session()).header(token.header(),token.value())
          .header("X-Queue-Token",order.token()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM push_subscription",Integer.class));verifyNoInteractions(provider);
  }
  @Test void detachingOneSharedBrowserSubscriptionPreservesOtherOrderDelivery() throws Exception {
    var first=create();var second=create();var token=csrf(null);attach(first,token);attach(second,token);
    var shared=jdbc.queryForObject("SELECT push_subscription_id FROM orders WHERE id=?",Long.class,first.id());
    assertEquals(shared,jdbc.queryForObject("SELECT push_subscription_id FROM orders WHERE id=?",Long.class,second.id()));
    String path="/api/v1/orders/"+first.id()+"/subscription";
    mvc.perform(delete(path).session(token.session()).header(token.header(),token.value()).header("X-Queue-Token",second.token())).andExpect(status().isForbidden());
    mvc.perform(delete(path).header("X-Queue-Token",first.token())).andExpect(status().isForbidden());
    mvc.perform(delete(path).session(token.session()).header(token.header(),token.value()).header("X-Queue-Token",first.token())).andExpect(status().isNoContent());
    assertNull(jdbc.queryForObject("SELECT push_subscription_id FROM orders WHERE id=?",Long.class,first.id()));
    assertEquals(shared,jdbc.queryForObject("SELECT push_subscription_id FROM orders WHERE id=?",Long.class,second.id()));
    assertTrue(jdbc.queryForObject("SELECT is_active FROM push_subscription WHERE id=?",Boolean.class,shared));
    when(provider.send(any(),any())).thenReturn(201);var staff=staff();
    advance(first,staff,"PREPARING");advance(first,staff,"READY");assertEquals(0,logs(first));verifyNoInteractions(provider);
    advance(second,staff,"PREPARING");advance(second,staff,"READY");assertEquals(1,logs(second));verify(provider,times(1)).send(any(),any());
  }
  @Test void endedOrderCannotRegisterAndItsHistoryRemainsReadOnly() throws Exception {
    var order=create();var staff=staff();advance(order,staff,"PREPARING");advance(order,staff,"READY");advance(order,staff,"COMPLETED");
    var token=csrf(null);mvc.perform(post("/api/v1/orders/"+order.id()+"/subscription").session(token.session()).header(token.header(),token.value())
        .header("X-Queue-Token",order.token()).contentType("application/json").content(json.writeValueAsString(subscription()))).andExpect(status().isConflict());
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM push_subscription",Integer.class));assertEquals(0,logs(order));verifyNoInteractions(provider);
  }
}
