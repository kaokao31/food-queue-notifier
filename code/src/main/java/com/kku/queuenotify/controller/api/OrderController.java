package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.common.PageRequests;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.*;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.exception.ApiException;
import java.util.Map;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderService orders;

  public OrderController(OrderService o) {
    orders = o;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest r) {
    var o = orders.create(r);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + o.id())).body(o);
  }

  @GetMapping("/{id}")
  public OrderResponse get(
      @PathVariable Long id,
      @RequestHeader(value = "X-Queue-Token", required = false) String token) {
    return orders.get(id, token);
  }

  @GetMapping
  public PageResponse<OrderResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort,
      @RequestParam(required = false) QueueStatus queueStatus) {
    return orders.list(
        queueStatus, PageRequests.of(page, size, sort, Set.of("id", "createdAt", "totalAmount")));
  }

  @PutMapping("/{id}")
  public OrderResponse update(
      @PathVariable Long id,
      @RequestHeader(value = "X-Queue-Token", required = false) String token,
      @Valid @RequestBody OrderRequest r) {
    return orders.update(id, token, r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    orders.delete(id);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,String>> error(ApiException error) {return ResponseEntity.status(error.getStatus()).body(Map.of("message",error.getMessage()));}
}
