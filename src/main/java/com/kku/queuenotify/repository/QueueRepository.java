package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.Queue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {
    Optional<Queue> findByOrderId(Long orderId);
}
