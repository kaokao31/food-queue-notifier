package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.common.PageRequests;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/menu-items")
@Tag(name = "Menu")
public class MenuController {
  private final MenuService menus;
  private final OrderAccessService access;

  public MenuController(MenuService m, OrderAccessService a) {
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
        includeUnavailable && access.isStaff());
  }

  @GetMapping("/{id}")
  public MenuItemResponse get(@PathVariable Long id) {
    return menus.get(id, access.isStaff());
  }

  @PostMapping
  public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest r) {
    var m = menus.create(r);
    return ResponseEntity.created(URI.create("/api/v1/menu-items/" + m.id())).body(m);
  }

  @PutMapping("/{id}")
  public MenuItemResponse update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest r) {
    return menus.update(id, r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    menus.delete(id);
    return ResponseEntity.noContent().build();
  }
}
