package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.service.QueueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queues")
@Tag(name = "Queues")
public class QueueController {
  private final QueueService queues;

  public QueueController(QueueService q) {
    queues = q;
  }

  @GetMapping("/{id}")
  public QueueResponse get(
      @PathVariable Long id,
      @RequestHeader(value = "X-Queue-Token", required = false) String token) {
    return queues.get(id, token);
  }

  @PatchMapping("/{id}/advance")
  public QueueResponse advance(@PathVariable Long id) {
    return queues.advance(id);
  }

  @PatchMapping("/{id}/cancel")
  public QueueResponse cancel(
      @PathVariable Long id,
      @RequestHeader(value = "X-Queue-Token", required = false) String token) {
    return queues.cancel(id, token);
  }
}
