package com.kku.queuenotify;

import com.kku.queuenotify.domain.entity.Order;
import com.kku.queuenotify.domain.entity.PushSubscription;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.PushSubscriptionRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.SubscriptionValidator;
import com.kku.queuenotify.service.impl.OrderSubscriptionServiceImpl;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderSubscriptionDetachTest {
  private final StaticListableBeanFactory beans = new StaticListableBeanFactory();
  private final OrderAccessService access = mock(OrderAccessService.class);
  private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private OrderSubscriptionServiceImpl service;
  private Order first;
  private Order second;
  private Queue queue;
  private PushSubscription shared;

  @BeforeEach void setup() {
    shared = new PushSubscription(); shared.setId(3L); shared.setActive(true);
    first = new Order(); first.setPushSubscription(shared);
    second = new Order(); second.setPushSubscription(shared);
    queue = Queue.builder().id(8L).status(QueueStatus.WAITING).order(first).build();
    service = new OrderSubscriptionServiceImpl(beans.getBeanProvider(OrderAccessService.class),
        repository, new SubscriptionValidator(), Clock.systemUTC(), events);
    when(access.locked(8L, "owner-fixture")).thenReturn(queue);
  }
  private void enableAccess() { beans.addBean("fixtureAccess", access); }

  @Test void removesOnlyAuthorizedOrderAssociationAndKeepsSharedSubscriptionActive() {
    enableAccess(); service.detach(8L, "owner-fixture");
    assertNull(first.getPushSubscription()); assertSame(shared, second.getPushSubscription());
    assertTrue(shared.isActive()); assertEquals(QueueStatus.WAITING,queue.getStatus());
    verify(access).locked(8L, "owner-fixture"); verifyNoInteractions(repository, events);
  }
  @Test void repeatedDetachAndAlreadyDetachedOrderAreIdempotent() {
    enableAccess(); first.setPushSubscription(null);
    service.detach(8L, "owner-fixture"); service.detach(8L, "owner-fixture");
    assertNull(first.getPushSubscription()); assertSame(shared,second.getPushSubscription());
    verify(access,times(2)).locked(8L,"owner-fixture"); verifyNoInteractions(repository,events);
  }
  @Test void missingAccessFailsClosedWithoutChangingAssociations() {
    ApiException ex=assertThrows(ApiException.class,()->service.detach(8L,"owner-fixture"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE,ex.getStatus());
    assertSame(shared,first.getPushSubscription()); verifyNoInteractions(access,repository,events);
  }
  @Test void deniedOrMissingOrderCannotChangeAnAssociation() {
    enableAccess();
    when(access.locked(8L,"wrong-fixture")).thenThrow(new ApiException(HttpStatus.FORBIDDEN,"denied"));
    when(access.locked(99L,"owner-fixture")).thenThrow(new ApiException(HttpStatus.NOT_FOUND,"Order not found"));
    assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,
        ()->service.detach(8L,"wrong-fixture")).getStatus());
    assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException.class,
        ()->service.detach(99L,"owner-fixture")).getStatus());
    assertSame(shared,first.getPushSubscription()); verifyNoInteractions(repository,events);
  }
  @Test void optOutIsAllowedInEveryQueueStateWithoutChangingState() {
    enableAccess();
    for (QueueStatus status:QueueStatus.values()) {
      queue.setStatus(status); first.setPushSubscription(shared);
      service.detach(8L,"owner-fixture");
      assertNull(first.getPushSubscription()); assertEquals(status,queue.getStatus());
      assertSame(shared,second.getPushSubscription()); assertTrue(shared.isActive());
    }
    verifyNoInteractions(repository,events);
  }
}
