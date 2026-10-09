package com.kku.queuenotify.service;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.*;
import org.springframework.data.domain.Pageable;

public interface OrderService {
  OrderResponse create(OrderRequest r);

  OrderResponse get(Long id, String token);

  OrderResponse update(Long id, String token, OrderRequest r);

  void delete(Long id);

  PageResponse<OrderResponse> list(QueueStatus status, Pageable p);
}
