package com.kku.queuenotify;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.impl.OrderAccessServiceImpl;
import com.kku.queuenotify.support.OrderingTestDoubles;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderAccessServiceTest {
  private final QueueToken tokens=new QueueToken();
  private final QueueRepository queues=mock(QueueRepository.class);
  private final OrderAccessServiceImpl access=new OrderAccessServiceImpl(queues,tokens);
  @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }
  private Queue fixture(String hash) {var queue=new Queue();queue.setId(8L);queue.setTokenHash(hash);when(queues.lockById(8L)).thenReturn(Optional.of(queue));return queue;}
  private void request(boolean principal,boolean role) {
    var request=new MockHttpServletRequest();if(principal)request.setUserPrincipal(()->"staff-fixture");
    if(role)request.addUserRole("STAFF");RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
  @Test void correctOwnerReceivesSameLockedQueue() {
    String token=tokens.generate();var queue=fixture(tokens.hash(token));
    assertSame(queue,access.locked(8L,token));verify(queues).lockById(8L);assertFalse(access.isStaff());
  }
  @Test void anotherOrdersTokenAndMissingOrMalformedTokenAreDenied() {
    String owner=tokens.generate();fixture(tokens.hash(owner));
    for(String token:new String[]{tokens.generate(),null,""," ","x".repeat(257)}) {
      var ex=assertThrows(ApiException.class,()->access.locked(8L,token));assertEquals(HttpStatus.FORBIDDEN,ex.getStatus());
      assertEquals("Order access denied",ex.getMessage());
    }
  }
  @Test void legacyMissingOrInvalidStoredHashDoesNotGrantOwnerAccess() {
    for(String hash:new String[]{null,"","legacy","x".repeat(64)}) {
      fixture(hash);assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->access.locked(8L,"fixture")).getStatus());
    }
  }
  @Test void authenticatedStaffCanAccessLegacyQueueWithoutToken() {
    var queue=fixture(null);request(true,true);assertTrue(access.isStaff());assertSame(queue,access.locked(8L,null));
  }
  @Test void principalAndStaffRoleAreBothRequiredAndNoRequestFailsClosed() {
    assertFalse(access.isStaff());fixture(null);
    for(boolean[] values:new boolean[][]{{false,true},{true,false},{false,false}}) {
      request(values[0],values[1]);assertFalse(access.isStaff());
      assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->access.locked(8L,null)).getStatus());
    }
  }
  @Test void missingOrInvalidIdGives404WithoutLeakingToken() {
    for(Long id:new Long[]{null,0L,-1L}) assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException.class,()->access.locked(id,"fixture-secret")).getStatus());
    verifyNoInteractions(queues);when(queues.lockById(8L)).thenReturn(Optional.empty());
    var ex=assertThrows(ApiException.class,()->access.locked(8L,"fixture-secret"));
    assertEquals(HttpStatus.NOT_FOUND,ex.getStatus());assertEquals("Order not found",ex.getMessage());
  }
  @Test void existingOrderingTestsUsePrimaryAccessFixtureAlongsideRealService() {
    new ApplicationContextRunner().withUserConfiguration(OrderAccessServiceImpl.class,QueueToken.class,OrderingTestDoubles.class)
        .withBean(QueueRepository.class,()->queues).run(ctx->{
          assertNull(ctx.getStartupFailure());
          assertSame(ctx.getBean(OrderingTestDoubles.Access.class),ctx.getBeanProvider(OrderAccessService.class).getIfAvailable());
          assertNotNull(ctx.getBean(OrderAccessServiceImpl.class));
        });
  }
}
