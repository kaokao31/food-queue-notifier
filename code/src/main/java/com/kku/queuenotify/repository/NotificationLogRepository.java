package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.NotificationLog;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
  List<NotificationLog> findByQueueIdOrderByIdDesc(Long id);

  boolean existsByQueueId(Long id);

  boolean existsByQueueIdAndEventType(Long id, String event);
}
