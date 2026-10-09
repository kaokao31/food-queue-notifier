package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.mapper.QueueMapper;
import java.time.LocalDateTime;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class QueueContractTest {
  @Test void mapsPublicQueueFieldsWithoutExposingTokenOrEntityRelationships() throws Exception {
    var changedAt = LocalDateTime.of(2026, 10, 9, 0, 0);
    var queue = Queue.builder().id(12L).queueNumber(7).queueDate(LocalDate.of(2026,10,9)).status(QueueStatus.WAITING)
        .statusChangedAt(changedAt).tokenHash("private-token-hash").build();
    var response = new QueueMapper().toResponse(queue);
    assertEquals(12L, response.id()); assertEquals(7, response.queueNumber());
    assertEquals(QueueStatus.WAITING, response.status()); assertEquals(changedAt, response.statusChangedAt());
    var json = new ObjectMapper().findAndRegisterModules().valueToTree(response);
    assertEquals(5, json.size()); assertEquals("WAITING", json.get("status").asText());
    assertEquals(LocalDate.of(2026,10,9),response.queueDate());
    assertFalse(json.has("tokenHash")); assertFalse(json.has("order")); assertFalse(json.has("notificationLogs"));
  }
}
