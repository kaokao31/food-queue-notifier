package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Terminal state: neither advance nor cancellation can mutate the queue. */
@Component
public final class CancelledState implements QueueStateHandler {
  @Override public QueueStatus getStatus(){return QueueStatus.CANCELLED;}
  @Override public void next(Context context){throw conflict();}
  @Override public void cancel(Context context){throw conflict();}
  private ApiException conflict(){return new ApiException(HttpStatus.CONFLICT,"Cancelled queue cannot change state");}
}
