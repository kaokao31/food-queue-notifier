package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.Order;
import com.kku.queuenotify.domain.enums.QueueStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @Query("select o from Order o join o.queue q where (:status is null or q.status=:status)")
  Page<Order> list(QueueStatus status, Pageable page);
}
