package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.repository.OrderRepository;
import com.kku.queuenotify.service.MenuService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
class OrderingIntegrationTest extends IntegrationTestSupport {
 @Autowired MenuService menus;@Autowired OrderRepository orders;@Autowired ObjectMapper json;
 String body(long id,int quantity){return "{\"items\":[{\"menuItemId\":"+id+",\"quantity\":"+quantity+"}]}";}
 @Test void realMvcPersistsSnapshotsAndUsesPerOrderAccess() throws Exception {
  var menu=menus.create(new MenuItemRequest("Original","FOOD",new BigDecimal("12.50"),10,true));
  mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body(menu.id(),0))).andExpect(status().isBadRequest());assertEquals(0,orders.count());
  var result=mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body(menu.id(),2))).andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(25)).andReturn();
  var created=json.readTree(result.getResponse().getContentAsString());long id=created.get("id").asLong();String token=created.get("queueToken").asText();
  menus.update(menu.id(),new MenuItemRequest("Changed","FOOD",new BigDecimal("99.00"),10,true));
  mvc.perform(get("/api/v1/orders/"+id).header("X-Queue-Token","wrong")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/orders/"+id).header("X-Queue-Token",token)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].menuItemName").value("Original")).andExpect(jsonPath("$.items[0].unitPrice").value(12.5)).andExpect(jsonPath("$.queueToken").doesNotExist());
  mvc.perform(put("/api/v1/orders/"+id).header("X-Queue-Token",token).contentType(MediaType.APPLICATION_JSON).content(body(menu.id(),1))).andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(99));
  mvc.perform(delete("/api/v1/orders/"+id)).andExpect(status().isForbidden());access.staff=true;mvc.perform(delete("/api/v1/orders/"+id)).andExpect(status().isNoContent());assertFalse(orders.existsById(id));
 }
 @Test void publicCatalogHidesClosedMenusAndStaffMutationsDelegateAccess() throws Exception {
  var open=menus.create(new MenuItemRequest("Open","FOOD",BigDecimal.TEN,10,true));menus.create(new MenuItemRequest("Closed","FOOD",BigDecimal.TEN,10,false));
  mvc.perform(get("/api/v1/menu-items")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(delete("/api/v1/menu-items/"+open.id())).andExpect(status().isForbidden());access.staff=true;mvc.perform(delete("/api/v1/menu-items/"+open.id())).andExpect(status().isNoContent());
 }
}
