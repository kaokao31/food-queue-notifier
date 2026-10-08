package com.kku.queuenotify.service;

import com.kku.queuenotify.domain.entity.Queue;

public interface OrderAccessService {
  Queue locked(Long id, String token);

  boolean isStaff();
}
