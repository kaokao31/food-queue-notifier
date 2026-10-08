package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.response.NotificationLogResponse;
import java.util.List;

public interface NotificationLogService {
  List<NotificationLogResponse> list(Long queueId);
}
