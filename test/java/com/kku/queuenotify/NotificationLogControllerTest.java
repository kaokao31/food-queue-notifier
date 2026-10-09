package com.kku.queuenotify;
import com.kku.queuenotify.controller.api.NotificationLogController;
import com.kku.queuenotify.domain.entity.NotificationLog;
import com.kku.queuenotify.domain.enums.NotificationChannel;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.impl.NotificationLogServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationLogControllerTest {
  @SuppressWarnings("unchecked") final ObjectProvider<OrderAccessService> providers=mock(ObjectProvider.class);
  final OrderAccessService access=mock(OrderAccessService.class);
  final QueueRepository queues=mock(QueueRepository.class);final NotificationLogRepository logs=mock(NotificationLogRepository.class);
  final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new NotificationLogController(new NotificationLogServiceImpl(providers,queues,logs))).build();
  @Test void unavailableAccessReturns503NoStore() throws Exception {
    mvc.perform(get("/api/v1/orders/8/notifications")).andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.status").value(503));verifyNoInteractions(queues,logs);
  }
  @Test void ownerTokenCannotSubstituteForStaffAccess() throws Exception {
    when(providers.getIfAvailable()).thenReturn(access);
    mvc.perform(get("/api/v1/orders/8/notifications").header("X-Queue-Token","owner-fixture"))
        .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control","no-store"));verifyNoInteractions(queues,logs);
  }
  @Test void staffCanReadOnlySafeMetadataWithNoStore() throws Exception {
    when(providers.getIfAvailable()).thenReturn(access);when(access.isStaff()).thenReturn(true);when(queues.existsById(8L)).thenReturn(true);
    var log=new NotificationLog();log.setId(42L);log.setChannel(NotificationChannel.PUSH);log.setDeliveryStatus("ACCEPTED");log.setEventType("READY");log.setHttpStatus(201);log.setMessage("SECRET_KEY_ENDPOINT");
    when(logs.findByQueueIdOrderByIdDesc(8L)).thenReturn(List.of(log));
    mvc.perform(get("/api/v1/orders/8/notifications")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
        .andExpect(jsonPath("$[0].deliveryStatus").value("ACCEPTED")).andExpect(jsonPath("$[0].httpStatus").value(201))
        .andExpect(jsonPath("$[0].endpoint").doesNotExist()).andExpect(jsonPath("$[0].token").doesNotExist())
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))));
  }
  @Test void missingAndEmptyOrderResponsesAreDistinct() throws Exception {
    when(providers.getIfAvailable()).thenReturn(access);when(access.isStaff()).thenReturn(true);
    mvc.perform(get("/api/v1/orders/99/notifications")).andExpect(status().isNotFound());
    when(queues.existsById(8L)).thenReturn(true);when(logs.findByQueueIdOrderByIdDesc(8L)).thenReturn(List.of());
    mvc.perform(get("/api/v1/orders/8/notifications")).andExpect(status().isOk()).andExpect(content().json("[]"));
    mvc.perform(get("/api/v1/orders/0/notifications")).andExpect(status().isBadRequest());
  }
}
