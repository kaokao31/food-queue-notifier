package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.response.QueueResponse;

/** Queue operations shared by the REST controller and transactional service. */
public interface QueueService {
  /** Read a queue after checking owner-token or staff access. */
  QueueResponse get(Long id, String token);

  /** Advance a queue after checking staff access and the current state. */
  QueueResponse advance(Long id);

  /** Cancel after checking owner-token or staff access and the current state. */
  QueueResponse cancel(Long id, String token);
}
