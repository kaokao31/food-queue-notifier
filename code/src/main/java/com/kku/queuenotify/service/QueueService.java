package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.response.QueueResponse;

public interface QueueService {
  QueueResponse get(Long id, String token);

  QueueResponse advance(Long id);

  QueueResponse cancel(Long id, String token);
}
