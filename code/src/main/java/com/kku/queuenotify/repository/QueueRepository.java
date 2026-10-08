package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.Queue;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface QueueRepository extends JpaRepository<Queue, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select q from Queue q where q.id=:id")
  Optional<Queue> lockById(Long id);
}
