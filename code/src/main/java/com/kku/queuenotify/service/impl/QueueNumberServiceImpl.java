package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.repository.DailyQueueCounterRepository;
import com.kku.queuenotify.service.QueueNumberService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueNumberServiceImpl implements QueueNumberService {
  private static final ZoneId QUEUE_ZONE = ZoneId.of("Asia/Bangkok");
  private final DailyQueueCounterRepository counters;
  private final Clock clock;

  public QueueNumberServiceImpl(DailyQueueCounterRepository counters, Clock clock) {
    this.counters = counters;
    this.clock = clock;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Number next() {
    var date = LocalDate.ofInstant(clock.instant(), QUEUE_ZONE);
    return new Number(date, counters.incrementAndGet(date));
  }
}
