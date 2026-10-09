package com.kku.queuenotify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PushDeliveryIntegrationTest extends PushApiTestSupport {
  @Test void fullApiWorkflowStoresAcceptanceAndOnlyStaffCanReadSafeLog() throws Exception {
    var order=create();var customer=csrf(null);attach(order,customer);
    when(provider.send(any(),any())).thenAnswer(call->{
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      assertEquals("READY",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));return 202;});
    var staff=staff();advance(order,staff,"PREPARING");assertEquals(0,logs(order));advance(order,staff,"READY");
    String path="/api/v1/orders/"+order.id()+"/notifications";
    mvc.perform(get(path).header("X-Queue-Token",order.token())).andExpect(status().isUnauthorized());
    var response=mvc.perform(get(path).session(staff.session())).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$[0].deliveryStatus").value("ACCEPTED"))
        .andExpect(jsonPath("$[0].httpStatus").value(202)).andExpect(jsonPath("$[0].eventType").value("READY"))
        .andExpect(jsonPath("$[0].channel").value("PUSH")).andExpect(jsonPath("$[0].sentAt").isNotEmpty()).andReturn();
    String body=response.getResponse().getContentAsString();
    for(var secret:List.of(order.token(),subscription().endpoint(),subscription().keys().auth(),subscription().keys().p256dh()))assertFalse(body.contains(secret));
    assertEquals(1,logs(order));verify(provider,times(1)).send(any(),any());
  }
  @Test void lateApiRegistrationCatchesUpButRepeatedRegistrationCannotResend() throws Exception {
    var order=create();var staff=staff();when(provider.send(any(),any())).thenReturn(201);
    advance(order,staff,"PREPARING");advance(order,staff,"READY");assertEquals(0,logs(order));verifyNoInteractions(provider);
    var customer=csrf(null);attach(order,customer);attach(order,customer);
    assertEquals(1,logs(order));verify(provider,times(1)).send(any(),any());
    mvc.perform(get("/api/v1/orders/"+order.id()+"/notifications").session(staff.session()))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].deliveryStatus").value("ACCEPTED"));
  }
  @Test void providerRejectionDoesNotFailReadyApiAndIsNotAutomaticallyRetried() throws Exception {
    var staff=staff();var customer=csrf(null);
    for(int code:new int[]{410,429,500}){
      var order=create();attach(order,customer);when(provider.send(any(),any())).thenReturn(code);
      advance(order,staff,"PREPARING");advance(order,staff,"READY");attach(order,customer);
      assertEquals(1,logs(order));mvc.perform(get("/api/v1/orders/"+order.id()+"/notifications").session(staff.session()))
          .andExpect(status().isOk()).andExpect(jsonPath("$[0].deliveryStatus").value("FAILED"))
          .andExpect(jsonPath("$[0].httpStatus").value(code)).andExpect(jsonPath("$[0].sentAt").isEmpty());
      assertEquals("READY",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id()));
    }verify(provider,times(3)).send(any(),any());
  }
  @Test void cancellationDoesNotSendOrCreateReadyLog() throws Exception {
    var order=create();var customer=csrf(null);attach(order,customer);
    mvc.perform(patch("/api/v1/queues/"+order.id()+"/cancel").session(customer.session()).header(customer.header(),customer.value())
        .header("X-Queue-Token",order.token())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    var staff=staff();mvc.perform(patch("/api/v1/queues/"+order.id()+"/advance").session(staff.session()).header(staff.header(),staff.value())).andExpect(status().isConflict());
    mvc.perform(get("/api/v1/orders/"+order.id()+"/notifications").session(staff.session())).andExpect(status().isOk()).andExpect(content().json("[]"));
    assertEquals(0,logs(order));verifyNoInteractions(provider);
  }
}
