package com.kku.queuenotify.domain.entity;

import com.kku.queuenotify.domain.enums.QueueStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "queue", uniqueConstraints = @UniqueConstraint(name = "uq_queue_number", columnNames = "queue_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Queue {

  @Column(name = "token_hash", length = 64, unique = true)
  private String tokenHash;

  @Id private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private Order order;

  @Column(name = "queue_number", nullable = false)
  private Integer queueNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private QueueStatus status;

  @Column(name = "status_changed_at")
  private LocalDateTime statusChangedAt;

  // 1:N
  @OneToMany(
      mappedBy = "queue",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @Builder.Default
  private List<NotificationLog> notificationLogs = new ArrayList<>();
}
