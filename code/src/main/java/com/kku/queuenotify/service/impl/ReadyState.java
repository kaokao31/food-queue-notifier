package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Stateless rules; QueueContext owns transactional mutation and event publication. */
@Component
public final class ReadyState implements QueueStateHandler {
  @Override public QueueStatus getStatus(){return QueueStatus.READY;}
  @Override public void next(Context context){requireCurrent(context);context.transitionTo(QueueStatus.COMPLETED);}
  @Override public void cancel(Context context){throw new ApiException(HttpStatus.CONFLICT,"Ready queue cannot be cancelled");}
  private void requireCurrent(Context context){
    if(context.getStatus()!=getStatus())throw new ApiException(HttpStatus.CONFLICT,"Queue state does not match this operation");
  }
}
