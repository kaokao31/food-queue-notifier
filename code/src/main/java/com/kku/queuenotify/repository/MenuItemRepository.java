package com.kku.queuenotify.repository;

import com.kku.queuenotify.domain.entity.MenuItem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
  Page<MenuItem> findByAvailableTrue(Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from MenuItem m where m.id=:id")
  Optional<MenuItem> lockById(Long id);
}
