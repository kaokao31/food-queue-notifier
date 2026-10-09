package com.kku.queuenotify.repository;

import java.time.LocalDate;
/** Allocates a daily number in the caller's order transaction. */
public interface DailyQueueCounterRepository {
  int incrementAndGet(LocalDate date);
}
