package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.Order;
import com.kku.queuenotify.domain.entity.OrderItem;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.OrderRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.mapper.OrderMapper;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
  private final OrderRepository orders;
  private final MenuItemRepository menus;
  private final QueueRepository queues;
  private final NotificationLogRepository logs;
  private final OrderAccessService access;
  private final QueueToken tokens;
  private final OrderMapper mapper;
  private final QueueNumberService queueNumbers;

  public OrderServiceImpl(
      OrderRepository o,
      MenuItemRepository m,
      QueueRepository q,
      NotificationLogRepository l,
      OrderAccessService a,
      QueueToken t,
      OrderMapper map,
      QueueNumberService queueNumbers) {
    orders = o;
    menus = m;
    queues = q;
    logs = l;
    access = a;
    tokens = t;
    mapper = map;
    this.queueNumbers = queueNumbers;
  }

  public OrderResponse create(OrderRequest r) {
    var o = new Order();
    replaceItems(o, r);
    var token = tokens.generate();
    var q = new Queue();
    q.setOrder(o);
    var number = queueNumbers.next();
    q.setQueueDate(number.date());
    q.setQueueNumber(number.value());
    q.setStatus(QueueStatus.WAITING);
    q.setTokenHash(tokens.hash(token));
    q.setStatusChangedAt(LocalDateTime.now(ZoneOffset.UTC));
    o.setQueue(q);
    orders.saveAndFlush(o);
    return mapper.response(o, token);
  }

  public OrderResponse get(Long id, String token) {
    return mapper.response(access.locked(id, token).getOrder(), null);
  }

  public OrderResponse update(Long id, String token, OrderRequest r) {
    var q = access.locked(id, token);
    if (q.getStatus() != QueueStatus.WAITING) throw conflict();
    var o = q.getOrder();
    o.getOrderItems().clear();
    orders.flush();
    replaceItems(o, r);
    o.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
    orders.saveAndFlush(o);
    return mapper.response(o, null);
  }

  public void delete(Long id) {
    var q = access.locked(id, null);
    if (q.getStatus() != QueueStatus.WAITING || logs.existsByQueueId(id)) throw conflict();
    orders.delete(q.getOrder());
    orders.flush();
  }

  @Transactional(readOnly = true)
  public PageResponse<OrderResponse> list(QueueStatus status, Pageable p) {
    return PageResponse.of(orders.list(status, p).map(o -> mapper.response(o, null)));
  }

  private void replaceItems(Order o, OrderRequest r) {
    var seen = new HashSet<Long>();
    var total = BigDecimal.ZERO;
    for (var line :
        r.items().stream().sorted(Comparator.comparing(OrderRequest.Item::menuItemId)).toList()) {
      if (!seen.add(line.menuItemId()))
        throw new ApiException(HttpStatus.BAD_REQUEST, "เมนูซ้ำในออเดอร์");
      var m =
          menus
              .lockById(line.menuItemId())
              .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ไม่พบเมนู"));
      if (!m.isAvailable()) throw new ApiException(HttpStatus.CONFLICT, "เมนูนี้ปิดขายแล้ว");
      var subtotal = m.getPrice().multiply(BigDecimal.valueOf(line.quantity()));
      var i = new OrderItem();
      i.setOrder(o);
      i.setMenuItem(m);
      i.setMenuItemName(m.getName());
      i.setUnitPrice(m.getPrice());
      i.setQuantity(line.quantity());
      i.setSubtotal(subtotal);
      o.getOrderItems().add(i);
      total = total.add(subtotal);
    }
    if (total.compareTo(new BigDecimal("99999999.99")) > 0)
      throw new ApiException(HttpStatus.BAD_REQUEST, "ยอดเงินเกินขอบเขต");
    o.setTotalAmount(total);
  }

  private ApiException conflict() {
    return new ApiException(
        HttpStatus.CONFLICT, "แก้ไขหรือลบได้เฉพาะคิวที่รอและยังไม่มีประวัติแจ้งเตือน");
  }
}
