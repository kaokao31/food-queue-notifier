package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.mapper.QueueMapper;
import com.kku.queuenotify.service.*;
import java.time.Clock;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QueueServiceImpl implements QueueService {
  private final OrderAccessService access;
  private final QueueMapper mapper;
  private final Map<QueueStatus,QueueStateHandler> states;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public QueueServiceImpl(OrderAccessService access,QueueMapper mapper,List<QueueStateHandler> handlers,
      ApplicationEventPublisher events,Clock clock) {
    this.access=Objects.requireNonNull(access);this.mapper=Objects.requireNonNull(mapper);
    this.events=Objects.requireNonNull(events);this.clock=Objects.requireNonNull(clock);
    var registry=new EnumMap<QueueStatus,QueueStateHandler>(QueueStatus.class);
    for(var handler:Objects.requireNonNull(handlers)) {
      if(handler==null || handler.getStatus()==null || registry.putIfAbsent(handler.getStatus(),handler)!=null)
        throw invalidRegistry();
    }
    if(!registry.keySet().equals(EnumSet.allOf(QueueStatus.class)))throw invalidRegistry();
    this.states=Map.copyOf(registry);
  }
  @Override public QueueResponse get(Long id,String token){return mapper.toResponse(access.locked(id,token));}
  @Override public QueueResponse advance(Long id){
    if(!access.isStaff())throw new ApiException(HttpStatus.FORBIDDEN,"Staff access is required");
    var queue=access.locked(id,null);context(queue).next();return mapper.toResponse(queue);
  }
  @Override public QueueResponse cancel(Long id,String token){
    var queue=access.locked(id,token);context(queue).cancel();return mapper.toResponse(queue);
  }
  private QueueContext context(Queue queue){
    if(queue.getStatus()==null)throw new ApiException(HttpStatus.CONFLICT,"Queue state is unavailable");
    return new QueueContext(queue,states.get(queue.getStatus()),events,clock);
  }
  private static IllegalArgumentException invalidRegistry(){
    return new IllegalArgumentException("Queue state handlers must cover every status exactly once");
  }
}
