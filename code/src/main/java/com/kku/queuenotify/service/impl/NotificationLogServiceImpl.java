package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.response.NotificationLogResponse;
import com.kku.queuenotify.repository.NotificationLogRepository;
import com.kku.queuenotify.service.NotificationLogService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationLogServiceImpl implements NotificationLogService {
  private final NotificationLogRepository logs;

  public NotificationLogServiceImpl(NotificationLogRepository l) {
    logs = l;
  }

  @Transactional(readOnly = true)
  public List<NotificationLogResponse> list(Long id) {
    return logs.findByQueueIdOrderByIdDesc(id).stream()
        .map(
            l ->
                new NotificationLogResponse(
                    l.getId(),
                    l.getMessage(),
                    l.getDeliveryStatus(),
                    l.getHttpStatus(),
                    l.getAttemptedAt()))
        .toList();
  }
}
