package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.OrderAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OrderAccessServiceImpl implements OrderAccessService {
  private final QueueRepository queues;
  private final QueueToken tokens;

  public OrderAccessServiceImpl(QueueRepository queues, QueueToken tokens) {
    this.queues = queues;
    this.tokens = tokens;
  }

  public boolean isStaff() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a != null
        && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_STAFF"));
  }

  public Queue locked(Long id, String token) {
    var q =
        queues
            .lockById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ไม่พบออเดอร์"));
    if (!isStaff() && !tokens.matches(token, q.getTokenHash()))
      throw new ApiException(HttpStatus.FORBIDDEN, "ไม่มีสิทธิ์เข้าถึงออเดอร์นี้");
    return q;
  }
}
