package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByQueueId(Long queueId);
}
