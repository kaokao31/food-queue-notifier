package com.kku.queuenotify.service;

import java.time.LocalDate;

/** Allocates a Bangkok daily number inside the caller's order transaction. */
public interface QueueNumberService {
  record Number(LocalDate date, int value) {}

  Number next();
}
