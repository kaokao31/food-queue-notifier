package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.mapper.QueueMapper;
import com.kku.queuenotify.service.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QueueServiceImpl implements QueueService {
  private final OrderAccessService access;
  private final QueueMapper mapper;
  private final ApplicationEventPublisher events;
  private final Map<QueueStatus, QueueStateHandler> states = new EnumMap<>(QueueStatus.class);

  public QueueServiceImpl(
      OrderAccessService access,
      QueueMapper mapper,
      ApplicationEventPublisher events,
      List<QueueStateHandler> handlers) {
    this.access = access;
    this.mapper = mapper;
    this.events = events;
    handlers.forEach(h -> states.put(h.getStatus(), h));
  }

  public QueueResponse get(Long id, String token) {
    return mapper.toResponse(access.locked(id, token));
  }

  public QueueResponse advance(Long id) {
    var q = access.locked(id, null);
    new QueueContext(q, states.get(q.getStatus()), events).next();
    return mapper.toResponse(q);
  }

  public QueueResponse cancel(Long id, String token) {
    var q = access.locked(id, token);
    new QueueContext(q, states.get(q.getStatus()), events).cancel();
    return mapper.toResponse(q);
  }
}
