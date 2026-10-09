package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.domain.entity.*;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.mapper.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
class OrderMappingTest {
 @Test void returnsPurchaseSnapshotsAndOnlyExplicitCreationToken(){
  var menu=MenuItem.builder().id(8L).name("Changed name").price(new BigDecimal("99.00")).build();
  var item=OrderItem.builder().menuItem(menu).menuItemName("Original name").quantity(2).unitPrice(new BigDecimal("12.50")).subtotal(new BigDecimal("25.00")).build();
  var queue=Queue.builder().id(4L).queueNumber(7).status(QueueStatus.WAITING).tokenHash("private-hash").build();
  var order=new Order();order.setId(4L);order.setOrderItems(List.of(item));order.setQueue(queue);order.setTotalAmount(new BigDecimal("25.00"));
  var mapper=new OrderMapper(new QueueMapper());var response=mapper.response(order,null);
  assertNull(response.queueToken());assertFalse(response.pushEnabled());
  assertEquals("Original name",response.items().get(0).menuItemName());assertEquals(new BigDecimal("12.50"),response.items().get(0).unitPrice());
  assertEquals(new BigDecimal("25.00"),response.totalAmount());assertEquals(8L,response.items().get(0).menuItemId());assertEquals(7,response.queue().queueNumber());
  assertEquals("creation-token",mapper.response(order,"creation-token").queueToken());
  String json=new ObjectMapper().valueToTree(response).toString();assertFalse(json.contains("private-hash"));assertFalse(json.contains("tokenHash"));assertFalse(json.contains("endpoint"));assertFalse(json.contains("p256dh"));
  var subscription=new PushSubscription();subscription.setActive(true);order.setPushSubscription(subscription);assertTrue(mapper.response(order,null).pushEnabled());
 }
}
