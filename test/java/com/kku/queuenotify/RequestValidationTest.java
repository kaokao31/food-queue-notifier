package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.common.PageRequests;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.PageResponse;
import com.kku.queuenotify.exception.ApiException;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

class RequestValidationTest {
  @Test void validatesOrderItemsAndQuantityLimits() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      for (int quantity : new int[]{1,99}) assertTrue(validator.validate(new OrderRequest(List.of(new OrderRequest.Item(1L,quantity)))).isEmpty());
      for (Integer quantity : Arrays.asList(null,0,100)) assertFalse(validator.validate(new OrderRequest(List.of(new OrderRequest.Item(1L,quantity)))).isEmpty());
      assertFalse(validator.validate(new OrderRequest(null)).isEmpty());
      assertFalse(validator.validate(new OrderRequest(List.of())).isEmpty());
      assertFalse(validator.validate(new OrderRequest(Arrays.asList((OrderRequest.Item)null))).isEmpty());
      assertFalse(validator.validate(new OrderRequest(List.of(new OrderRequest.Item(0L,1)))).isEmpty());
    }
  }
  @Test void validatesMenuPriceNameAndPreparationTime() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      assertTrue(validator.validate(new MenuItemRequest("Rice",null,new BigDecimal("12.50"),0,true)).isEmpty());
      for (String price : new String[]{"-1.00","1.001","100000000.00"}) assertFalse(validator.validate(new MenuItemRequest("Rice",null,new BigDecimal(price),10,true)).isEmpty());
      assertFalse(validator.validate(new MenuItemRequest(" ",null,BigDecimal.ZERO,241,null)).isEmpty());
      assertFalse(validator.validate(new MenuItemRequest("x".repeat(101),null,null,-1,true)).isEmpty());
    }
  }
  @Test void restrictsPaginationSortAndAddsDeterministicIdTieBreaker() {
    var allowed = Set.of("id","name","price");
    var page = PageRequests.of(2,8,"price,desc",allowed);
    assertEquals(2,page.getPageNumber()); assertEquals(8,page.getPageSize());
    assertTrue(page.getSort().getOrderFor("price").isDescending());assertNotNull(page.getSort().getOrderFor("id"));
    for (String sort : Arrays.asList(null,"price","price,invalid","tokenHash,asc","name,asc,extra")) {
      var error = assertThrows(ApiException.class,()->PageRequests.of(0,8,sort,allowed));
      assertEquals(HttpStatus.BAD_REQUEST,error.getStatus());
    }
    assertThrows(ApiException.class,()->PageRequests.of(-1,8,"id,asc",allowed));
    assertThrows(ApiException.class,()->PageRequests.of(0,101,"id,asc",allowed));
    var response=PageResponse.of(new PageImpl<>(List.of("Rice"),PageRequest.of(0,8),9));
    assertEquals(2,response.totalPages());assertEquals(9,response.totalElements());assertEquals(List.of("Rice"),response.content());
  }
}
