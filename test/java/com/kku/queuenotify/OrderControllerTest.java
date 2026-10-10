package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kku.queuenotify.controller.api.OrderController;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class OrderControllerTest {
 String seen;int writes;
 OrderResponse response(String token){return new OrderResponse(4L,BigDecimal.TEN,List.of(),null,token,false,null,null);}
 // Test-only order service verifies MVC delegation; production access stays in OrderServiceImpl.
 OrderService fixture(){return new OrderService(){
  public OrderResponse create(OrderRequest r){writes++;return response("fixture-token");}
  public OrderResponse get(Long id,String token){seen=token;return response(null);}
  public OrderResponse update(Long id,String token,OrderRequest r){seen=token;writes++;return response(null);}
  public void delete(Long id){throw new ApiException(HttpStatus.FORBIDDEN,"staff only");}
  public PageResponse<OrderResponse> list(QueueStatus status,Pageable p){return new PageResponse<>(List.of(response(null)),p.getPageNumber(),p.getPageSize(),1,1);}
 };}
 @Test void validatesCreateAndForwardsPerBillToken() throws Exception {
  var mvc=MockMvcBuilders.standaloneSetup(new OrderController(fixture())).build();
  mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());assertEquals(0,writes);
  var body="{\"items\":[{\"menuItemId\":2,\"quantity\":1}]}";
  mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/orders/4")).andExpect(jsonPath("$.queueToken").value("fixture-token"));
  mvc.perform(get("/api/v1/orders/4").header("X-Queue-Token","bill-secret")).andExpect(status().isOk());assertEquals("bill-secret",seen);
  mvc.perform(put("/api/v1/orders/4").header("X-Queue-Token","bill-secret").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());assertEquals("bill-secret",seen);assertEquals(2,writes);
 }
 @Test void validatesPagingAndReportsDomainErrorWithoutSubscriptionEndpoints() throws Exception {
  var mvc=MockMvcBuilders.standaloneSetup(new OrderController(fixture())).build();mvc.perform(get("/api/v1/orders").param("size","101")).andExpect(status().isBadRequest());mvc.perform(delete("/api/v1/orders/4")).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("staff only"));mvc.perform(put("/api/v1/orders/4/push-subscription")).andExpect(status().isNotFound());
 }
}
