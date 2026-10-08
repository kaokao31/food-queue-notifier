package com.kku.queuenotify.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "queue_daily_counter")
@Getter
@Setter
@NoArgsConstructor
public class DailyQueueCounter {
  @Id
  @Column(name = "queue_date")
  private LocalDate queueDate;

  @Column(name = "last_number", nullable = false)
  private Integer lastNumber;
}
