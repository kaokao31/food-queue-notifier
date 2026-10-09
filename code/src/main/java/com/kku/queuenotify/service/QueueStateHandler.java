package com.kku.queuenotify.service;

import com.kku.queuenotify.domain.enums.QueueStatus;

/** State-specific rules; the context owns mutation and transition events. */
public interface QueueStateHandler {
  QueueStatus getStatus();

  /** Advance, or reject the operation when this state does not allow it. */
  void next(Context context);

  /** Cancel, or reject the operation when this state does not allow it. */
  void cancel(Context context);

  /** Minimal port implemented by the queue context in the next development steps. */
  interface Context {
    QueueStatus getStatus();

    void transitionTo(QueueStatus status);
  }
}
