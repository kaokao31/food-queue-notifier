package com.kku.queuenotify.domain.entity;

import com.kku.queuenotify.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

  @Column(name = "delivery_status", nullable = false, length = 20)
  private String deliveryStatus;

  @Column(name = "event_type", nullable = false, length = 30)
  private String eventType;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "attempted_at")
  private LocalDateTime attemptedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "queue_id", nullable = false)
  private Queue queue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationChannel channel;

  @Column(nullable = false, length = 255)
  private String message;

  @Column(nullable = false)
  private boolean success;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;
}
