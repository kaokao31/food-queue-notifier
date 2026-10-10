package com.kku.queuenotify.repository;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcDailyQueueCounterRepository implements DailyQueueCounterRepository {
  private final JdbcTemplate jdbc;

  public JdbcDailyQueueCounterRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  // UPDATE holds the day's row lock until the order transaction ends. A rollback
  // restores the counter together with the order; deletion never decrements it.
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public int incrementAndGet(LocalDate date) {
    jdbc.update(
        "INSERT INTO queue_daily_counter(queue_date,last_number) VALUES (?,0) ON CONFLICT DO"
            + " NOTHING",
        date);
    jdbc.update("UPDATE queue_daily_counter SET last_number=last_number+1 WHERE queue_date=?", date);
    return jdbc.queryForObject(
        "SELECT last_number FROM queue_daily_counter WHERE queue_date=?", Integer.class, date);
  }
}
