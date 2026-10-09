package com.kku.queuenotify;

import com.kku.queuenotify.domain.entity.Order;
import com.kku.queuenotify.domain.entity.PushSubscription;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.PushSubscriptionRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.SubscriptionValidator;
import com.kku.queuenotify.service.impl.OrderSubscriptionAttachedEvent;
import com.kku.queuenotify.service.impl.OrderSubscriptionServiceImpl;
import java.time.*;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderSubscriptionServiceTest {
  private final StaticListableBeanFactory beans = new StaticListableBeanFactory();
  private final OrderAccessService access = mock(OrderAccessService.class);
  private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC);
  private Queue queue;
  private OrderSubscriptionServiceImpl service;
  private static final String ENDPOINT = "https://fcm.googleapis.com/wp/test-fixture";
  static final String AUTH = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
  private PushSubscriptionRequest request(String auth) {
    return new PushSubscriptionRequest(ENDPOINT,
        new PushSubscriptionRequest.Keys(PushConfigurationTest.KEY, auth));
  }

  @BeforeEach void setup() {
    queue = Queue.builder().id(8L).status(QueueStatus.WAITING).order(new Order()).build();
    service = new OrderSubscriptionServiceImpl(beans.getBeanProvider(OrderAccessService.class),
        repository, new SubscriptionValidator(), clock, events);
    when(access.locked(8L, "owner-fixture")).thenReturn(queue);
    when(repository.findByEndpointHash(anyString())).thenReturn(Optional.empty());
  }
  private void enableAccess() { beans.addBean("fixtureAccess", access); }
  private void fails(HttpStatus status, Runnable operation) {
    assertEquals(status, assertThrows(ApiException.class, operation::run).getStatus());
  }

  @Test void missingAccessFailsClosedWithoutRepositoryWrites() {
    fails(HttpStatus.SERVICE_UNAVAILABLE, () -> service.attach(8L, "owner-fixture", request(AUTH)));
    verifyNoInteractions(repository, events);
  }

  @Test void rejectedOwnerAndEndedOrdersCannotAttach() {
    enableAccess();
    when(access.locked(8L, "wrong-fixture")).thenThrow(new ApiException(HttpStatus.FORBIDDEN, "denied"));
    fails(HttpStatus.FORBIDDEN, () -> service.attach(8L, "wrong-fixture", request(AUTH)));
    for (QueueStatus status : new QueueStatus[] {QueueStatus.COMPLETED, QueueStatus.CANCELLED}) {
      queue.setStatus(status);
      fails(HttpStatus.CONFLICT, () -> service.attach(8L, "owner-fixture", request(AUTH)));
    }
    verifyNoInteractions(repository, events);
  }

  @Test void createsCanonicalSubscriptionAndAssociatesOnlyAuthorizedOrder() {
    enableAccess();
    service.attach(8L, "owner-fixture", request(AUTH + "=="));
    PushSubscription saved = queue.getOrder().getPushSubscription();
    assertNotNull(saved);assertTrue(saved.isActive());assertEquals(AUTH, saved.getAuth());
    assertEquals(PushConfigurationTest.KEY, saved.getP256dh());assertEquals(64, saved.getEndpointHash().length());
    assertEquals(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), saved.getCreatedAt());
    verify(repository).saveAndFlush(saved);verifyNoInteractions(events);
  }

  @Test void reusesSameEndpointAndKeysWithoutChangingCreatedTime() {
    enableAccess();
    PushSubscription existing = new PushSubscription();existing.setId(3L);
    existing.setEndpoint(ENDPOINT);existing.setP256dh(PushConfigurationTest.KEY + "=");
    existing.setAuth(AUTH + "==");existing.setActive(false);
    LocalDateTime originalTime=LocalDateTime.of(2025,1,1,0,0);existing.setCreatedAt(originalTime);
    when(repository.findByEndpointHash(anyString())).thenReturn(Optional.of(existing));
    service.attach(8L, "owner-fixture", request(AUTH));
    assertSame(existing,queue.getOrder().getPushSubscription());assertTrue(existing.isActive());
    assertEquals(originalTime,existing.getCreatedAt());assertEquals(AUTH,existing.getAuth());
  }

  @Test void changedKeysOrInvalidEndpointDoNotWrite() {
    enableAccess();
    PushSubscription existing = new PushSubscription();existing.setEndpoint(ENDPOINT);
    existing.setP256dh(PushConfigurationTest.KEY);byte[] changed=new byte[16];changed[0]=1;
    existing.setAuth(Base64.getUrlEncoder().withoutPadding().encodeToString(changed));
    when(repository.findByEndpointHash(anyString())).thenReturn(Optional.of(existing));
    fails(HttpStatus.CONFLICT, () -> service.attach(8L,"owner-fixture",request(AUTH)));
    fails(HttpStatus.BAD_REQUEST, () -> service.attach(8L,"owner-fixture",
        new PushSubscriptionRequest("https://localhost/wp/fixture",request(AUTH).keys())));
    verify(repository,never()).saveAndFlush(any());assertNull(queue.getOrder().getPushSubscription());
    verifyNoInteractions(events);
  }

  @Test void readyAttachmentPublishesOnlyImmutableCatchUpRequest() {
    enableAccess();queue.setStatus(QueueStatus.READY);
    service.attach(8L,"owner-fixture",request(AUTH));
    var capture=org.mockito.ArgumentCaptor.forClass(Object.class);
    verify(events).publishEvent(capture.capture());
    OrderSubscriptionAttachedEvent event=assertInstanceOf(OrderSubscriptionAttachedEvent.class,capture.getValue());
    assertEquals(8L,event.orderId());assertNotNull(event.eventId());assertEquals(QueueStatus.READY,queue.getStatus());
  }
}
