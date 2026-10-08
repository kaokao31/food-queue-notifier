package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.response.NotificationLogResponse;
import com.kku.queuenotify.service.NotificationLogService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationLogController {
  private final NotificationLogService service;

  public NotificationLogController(NotificationLogService s) {
    service = s;
  }

  @GetMapping
  public List<NotificationLogResponse> list(@RequestParam Long queueId) {
    return service.list(queueId);
  }
}
