package com.kku.queuenotify;

import com.kku.queuenotify.common.QueueToken;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real application, filters, owner access, transactions and PostgreSQL.
 * Inherits test-only provider transport replacement; these cases never reach READY. */
class OrderAccessIntegrationTest extends PushApiTestSupport {
  String edit(Owner order,int quantity){
    Long menu=jdbc.queryForObject("SELECT menu_item_id FROM order_item WHERE order_id=?",Long.class,order.id());
    return "{\"items\":[{\"menuItemId\":"+menu+",\"quantity\":"+quantity+"}]}";
  }
  int quantity(Owner order){return jdbc.queryForObject("SELECT quantity FROM order_item WHERE order_id=?",Integer.class,order.id());}
  String state(Owner order){return jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,order.id());}
  void deny(MockHttpServletRequestBuilder request) throws Exception {
    mvc.perform(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value("Order access denied")).andExpect(header().string("Cache-Control","no-store"));
  }
  @Test void createdBillsPersistDistinctHashesAndReadsNeverReturnCredentials() throws Exception {
    var one=create();var two=create();assertNotEquals(one.token(),two.token());
    for(var order:List.of(one,two)){
      var hash=jdbc.queryForObject("SELECT token_hash FROM queue WHERE id=?",String.class,order.id());
      assertEquals(new QueueToken().hash(order.token()),hash);assertNotEquals(order.token(),hash);
      for(String path:List.of("/api/v1/orders/","/api/v1/queues/"))
        mvc.perform(get(path+order.id()).header("X-Queue-Token",order.token())).andExpect(status().isOk())
            .andExpect(jsonPath("$.queueToken").doesNotExist()).andExpect(jsonPath("$.tokenHash").doesNotExist());
    }
    verifyNoInteractions(provider);
  }
  @Test void otherBillTokenCannotReadEditOrCancelAndNeitherBillChanges() throws Exception {
    var one=create();var two=create();var csrf=csrf(null);
    for(var target:List.of(one,two)){
      var wrong=target.id().equals(one.id())?two:one;
      for(String path:List.of("/api/v1/orders/","/api/v1/queues/"))deny(get(path+target.id()).header("X-Queue-Token",wrong.token()));
      deny(put("/api/v1/orders/"+target.id()).session(csrf.session()).header(csrf.header(),csrf.value())
          .header("X-Queue-Token",wrong.token()).contentType("application/json").content(edit(target,2)));
      deny(patch("/api/v1/queues/"+target.id()+"/cancel").session(csrf.session()).header(csrf.header(),csrf.value()).header("X-Queue-Token",wrong.token()));
      assertEquals(1,quantity(target));assertEquals("WAITING",state(target));
    }
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification_log",Integer.class));verifyNoInteractions(provider);
  }
  @Test void absentAndMalformedTokensFailClosedEvenWithValidCsrf() throws Exception {
    var order=create();var csrf=csrf(null);
    for(String invalid:new String[]{null,"wrong",order.token()+"x"}){
      var read=get("/api/v1/orders/"+order.id());
      var update=put("/api/v1/orders/"+order.id()).session(csrf.session()).header(csrf.header(),csrf.value()).contentType("application/json").content(edit(order,2));
      var cancel=patch("/api/v1/queues/"+order.id()+"/cancel").session(csrf.session()).header(csrf.header(),csrf.value());
      if(invalid!=null){read.header("X-Queue-Token",invalid);update.header("X-Queue-Token",invalid);cancel.header("X-Queue-Token",invalid);}
      deny(read);deny(update);deny(cancel);
    }
    assertEquals(1,quantity(order));assertEquals("WAITING",state(order));verifyNoInteractions(provider);
  }
  @Test void correctOwnerCanEditAndCancelOnlyItsBillWithoutStaffPrivileges() throws Exception {
    var one=create();var two=create();var csrf=csrf(null);
    mvc.perform(put("/api/v1/orders/"+one.id()).session(csrf.session()).header(csrf.header(),csrf.value())
        .header("X-Queue-Token",one.token()).contentType("application/json").content(edit(one,2))).andExpect(status().isOk());
    assertEquals(2,quantity(one));assertEquals(1,quantity(two));
    for(String route:List.of("/api/v1/orders","/api/v1/orders/"+one.id()+"/notifications"))
      mvc.perform(get(route).session(csrf.session()).header("X-Queue-Token",one.token())).andExpect(status().isUnauthorized());
    mvc.perform(patch("/api/v1/queues/"+one.id()+"/advance").session(csrf.session()).header(csrf.header(),csrf.value())
        .header("X-Queue-Token",one.token())).andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/v1/orders/"+one.id()).session(csrf.session()).header(csrf.header(),csrf.value())
        .header("X-Queue-Token",one.token())).andExpect(status().isUnauthorized());
    mvc.perform(patch("/api/v1/queues/"+one.id()+"/cancel").session(csrf.session()).header(csrf.header(),csrf.value())
        .header("X-Queue-Token",one.token())).andExpect(status().isOk());
    assertEquals("CANCELLED",state(one));assertEquals("WAITING",state(two));assertEquals(1,quantity(two));verifyNoInteractions(provider);
  }
  @Test void legacyNullHashRejectsCustomersButAuthenticatedStaffCanManageIt() throws Exception {
    var legacy=create();var other=create();jdbc.update("UPDATE queue SET token_hash=NULL WHERE id=?",legacy.id());var csrf=csrf(null);
    for(String value:List.of(legacy.token(),other.token())){
      deny(get("/api/v1/orders/"+legacy.id()).header("X-Queue-Token",value));
      deny(patch("/api/v1/queues/"+legacy.id()+"/cancel").session(csrf.session()).header(csrf.header(),csrf.value()).header("X-Queue-Token",value));
    }
    assertEquals("WAITING",state(legacy));var staff=staff();
    mvc.perform(get("/api/v1/orders/"+legacy.id()).session(staff.session())).andExpect(status().isOk());
    mvc.perform(get("/api/v1/queues/"+legacy.id()).session(staff.session())).andExpect(status().isOk());
    mvc.perform(put("/api/v1/orders/"+legacy.id()).session(staff.session()).header(staff.header(),staff.value())
        .contentType("application/json").content(edit(legacy,2))).andExpect(status().isOk());
    mvc.perform(patch("/api/v1/queues/"+legacy.id()+"/cancel").session(staff.session()).header(staff.header(),staff.value())).andExpect(status().isOk());
    assertEquals(2,quantity(legacy));assertEquals("CANCELLED",state(legacy));assertEquals("WAITING",state(other));verifyNoInteractions(provider);
  }
}
