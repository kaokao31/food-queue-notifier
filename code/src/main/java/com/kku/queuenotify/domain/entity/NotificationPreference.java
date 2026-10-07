package com.kku.queuenotify.domain.entity;

import com.kku.queuenotify.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1:1 — customer_id เป็น UNIQUE FK เพื่อบังคับ 1:1 จริง
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "contact_value", nullable = false, length = 150)
    private String contactValue; // LINE user id / device token / เบอร์โทร แล้วแต่ channel

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
