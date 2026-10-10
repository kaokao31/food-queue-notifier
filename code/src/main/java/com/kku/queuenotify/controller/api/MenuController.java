package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.exception.GlobalExceptionHandler;
import com.kku.queuenotify.common.PageRequests;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import com.kku.queuenotify.exception.ApiException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/menu-items")
public class MenuController {
  private final MenuService menus;
  private final ObjectProvider<OrderAccessService> access;

  public MenuController(MenuService m, ObjectProvider<OrderAccessService> a) {
    menus = m;
    access = a;
  }

  @GetMapping
  public PageResponse<MenuItemResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "name,asc") String sort,
      @RequestParam(defaultValue = "false") boolean includeUnavailable) {
    return menus.list(
        PageRequests.of(page, size, sort, Set.of("id", "name", "price", "category")),
        includeUnavailable && isStaff());
  }

  @GetMapping("/{id}")
  public MenuItemResponse get(@PathVariable Long id) {
    return menus.get(id, isStaff());
  }

  @PostMapping
  public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest r) {
    requireStaff();
    var m = menus.create(r);
    return ResponseEntity.created(URI.create("/api/v1/menu-items/" + m.id())).body(m);
  }

  @PutMapping("/{id}")
  public MenuItemResponse update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest r) {
    requireStaff();
    return menus.update(id, r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    requireStaff();
    menus.delete(id);
    return ResponseEntity.noContent().build();
  }
  private boolean isStaff() {
    var service=access.getIfAvailable();
    return service!=null && service.isStaff();
  }

  private void requireStaff() {
    var service=access.getIfAvailable();
    if(service==null) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ระบบสิทธิ์พนักงานยังไม่พร้อมใช้งาน");
    if(!service.isStaff()) throw new ApiException(HttpStatus.FORBIDDEN,"สำหรับพนักงานเท่านั้น");
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,Object>> domainError(ApiException error) {
    return GlobalExceptionHandler.response(error.getStatus(),error.getMessage());
  }
}
