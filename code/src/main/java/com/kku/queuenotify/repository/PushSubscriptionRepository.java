package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.PushSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
  Optional<PushSubscription> findByEndpointHash(String hash);
}
