package com.kku.queuenotify.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "push_subscription")
@Getter
@Setter
@NoArgsConstructor
public class PushSubscription {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, columnDefinition = "text")
  private String endpoint;

  @Column(name = "endpoint_hash", nullable = false, length = 64, unique = true)
  private String endpointHash;

  @Column(nullable = false, columnDefinition = "text")
  private String p256dh;

  @Column(nullable = false, columnDefinition = "text")
  private String auth;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
