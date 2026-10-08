package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  boolean existsByMenuItemId(Long id);
}
