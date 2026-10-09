package com.kku.queuenotify;
import com.kku.queuenotify.domain.entity.NotificationLog;
import com.kku.queuenotify.domain.enums.NotificationChannel;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.impl.NotificationLogServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationLogServiceTest {
  @SuppressWarnings("unchecked") final ObjectProvider<OrderAccessService> providers=mock(ObjectProvider.class);
  final OrderAccessService access=mock(OrderAccessService.class);
  final QueueRepository queues=mock(QueueRepository.class);
  final NotificationLogRepository logs=mock(NotificationLogRepository.class);
  final NotificationLogServiceImpl service=new NotificationLogServiceImpl(providers,queues,logs);
  @BeforeEach void setup(){when(providers.getIfAvailable()).thenReturn(access);when(access.isStaff()).thenReturn(true);when(queues.existsById(8L)).thenReturn(true);}
  @Test void absentAccessFailsClosedBeforeAnyLookup(){when(providers.getIfAvailable()).thenReturn(null);
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException.class,()->service.forStaff(8L)).getStatus());verifyNoInteractions(queues,logs);}
  @Test void nonStaffIsDeniedEvenForMissingOrInvalidOrder(){when(access.isStaff()).thenReturn(false);
    for(Long id:new Long[]{8L,99L,0L,null})assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->service.forStaff(id)).getStatus());
    verifyNoInteractions(queues,logs);verify(access,never()).locked(any(),any());}
  @Test void staffInvalidIdIsRejectedBeforeRepositoryLookup(){for(Long id:new Long[]{0L,-1L,null})assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException.class,()->service.forStaff(id)).getStatus());verifyNoInteractions(queues,logs);}
  @Test void missingOrderAndEmptyHistoryRemainDistinct(){assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException.class,()->service.forStaff(99L)).getStatus());verifyNoInteractions(logs);
    when(logs.findByQueueIdOrderByIdDesc(8L)).thenReturn(List.of());assertTrue(service.forStaff(8L).isEmpty());}
  @Test void knownStatusesMapToSafeMessagesAndKeepRepositoryOrder(){
    var time=LocalDateTime.parse("2026-10-10T00:00:00");var rows=new java.util.ArrayList<NotificationLog>();
    for(String status:new String[]{"ACCEPTED","FAILED","PREVIEW","PENDING"}){
      var log=new NotificationLog();log.setId((long)rows.size()+1);log.setDeliveryStatus(status);log.setChannel(NotificationChannel.PUSH);
      log.setEventType("READY");log.setHttpStatus(201);log.setMessage("SECRET_ENDPOINT_AUTH");log.setAttemptedAt(time);rows.add(log);
    }
    when(logs.findByQueueIdOrderByIdDesc(8L)).thenReturn(rows);var results=service.forStaff(8L);
    assertEquals(List.of("ACCEPTED","FAILED","PREVIEW","PENDING"),results.stream().map(r->r.deliveryStatus()).toList());
    assertTrue(results.get(0).message().contains("ยังไม่ยืนยัน"));
    for(var result:results){assertFalse(result.message().contains("SECRET"));assertEquals(time,result.attemptedAt());assertEquals(201,result.httpStatus());assertEquals("READY",result.eventType());}
  }
  @Test void legacyAndInvalidValuesDoNotExposeRawText(){
    var log=new NotificationLog();log.setId(1L);log.setDeliveryStatus("SECRET");log.setEventType("SECRET");log.setMessage("SECRET_KEY");log.setHttpStatus(999);
    when(logs.findByQueueIdOrderByIdDesc(8L)).thenReturn(List.of(log));var response=service.forStaff(8L).get(0);
    assertEquals("LEGACY",response.deliveryStatus());assertEquals("LEGACY",response.eventType());assertEquals("UNKNOWN",response.channel());assertNull(response.httpStatus());assertFalse(response.message().contains("SECRET"));
    log.setDeliveryStatus(null);assertEquals("LEGACY",service.forStaff(8L).get(0).deliveryStatus());
  }
}
