package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.service.impl.OrderServiceImpl;
import com.kku.queuenotify.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
class OrderServiceAvailabilityTest {
 @Test void refusesOperationsWithoutAccessAndTokenProvidersBeforeTouchingDatabase(){
  var beans=new StaticListableBeanFactory();var service=new OrderServiceImpl(null,null,null,null,beans.getBeanProvider(OrderAccessService.class),beans.getBeanProvider(QueueTokenGenerator.class),null,null);
  assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException.class,()->service.create(null)).getStatus());assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException.class,()->service.get(1L,"token")).getStatus());assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException.class,()->service.update(1L,"token",null)).getStatus());assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException.class,()->service.delete(1L)).getStatus());assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException.class,()->service.list(null,null)).getStatus());
 }
}
