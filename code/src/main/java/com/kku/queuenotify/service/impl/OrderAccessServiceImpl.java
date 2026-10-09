package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.OrderAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OrderAccessServiceImpl implements OrderAccessService {
  private final QueueRepository queues;
  private final QueueToken tokens;
  public OrderAccessServiceImpl(QueueRepository queues, QueueToken tokens) {
    this.queues=queues; this.tokens=tokens;
  }

  @Override public boolean isStaff() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return false;
    var request=attributes.getRequest();
    // Spring Security's servlet wrapper supplies principal and roles once C-R04 configures it.
    // Before then no authenticated STAFF principal is supplied by this application.
    return request.getUserPrincipal()!=null && request.isUserInRole("STAFF");
  }

  /** The caller's transaction must retain the lock through subsequent mutation/flush. */
  @Override
  @Transactional(propagation=Propagation.MANDATORY)
  public Queue locked(Long id,String token) {
    if (id==null || id<=0) throw notFound();
    var queue=queues.lockById(id).orElseThrow(this::notFound);
    if (!isStaff() && !tokens.matches(token,queue.getTokenHash())) {
      throw new ApiException(HttpStatus.FORBIDDEN,"Order access denied");
    }
    return queue;
  }

  private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND,"Order not found"); }
}
