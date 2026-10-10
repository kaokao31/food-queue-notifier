package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.common.MenuPhotos;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.mapper.MenuItemMapper;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.MenuService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuServiceImpl implements MenuService {
  private final MenuItemRepository menus;
  private final OrderItemRepository lines;
  private final MenuItemMapper mapper;
  private final MenuImageRepository images;

  public MenuServiceImpl(
      MenuItemRepository menus,
      OrderItemRepository lines,
      MenuItemMapper mapper,
      MenuImageRepository images) {
    this.menus = menus;
    this.lines = lines;
    this.mapper = mapper;
    this.images = images;
  }

  @Transactional(readOnly = true)
  public PageResponse<MenuItemResponse> list(Pageable p, boolean all) {
    return PageResponse.of(
        (all ? menus.findAll(p) : menus.findByAvailableTrue(p)).map(mapper::response));
  }

  @Transactional(readOnly = true)
  public MenuItemResponse get(Long id, boolean all) {
    var m = menus.findById(id).orElseThrow(() -> missing());
    if (!all && !m.isAvailable()) throw missing();
    return mapper.response(m);
  }

  public MenuItemResponse create(MenuItemRequest r) {
    return save(new MenuItem(), r);
  }

  public MenuItemResponse update(Long id, MenuItemRequest r) {
    return save(menus.lockById(id).orElseThrow(() -> missing()), r);
  }

  private MenuItemResponse save(MenuItem m, MenuItemRequest r) {
    if (m.getImageKey() == null) {
      m.setImageKey(MenuPhotos.initialKey(m.getId() == null ? r.name() : m.getName(),
          m.getId() == null ? r.category() : m.getCategory()));
    }
    m.setName(r.name().trim());
    m.setCategory(r.category());
    m.setPrice(r.price());
    m.setPrepTimeMinutes(r.prepTimeMinutes());
    m.setAvailable(r.isAvailable());
    return mapper.response(menus.save(m));
  }

  public void delete(Long id) {
    var m = menus.lockById(id).orElseThrow(() -> missing());
    if (lines.existsByMenuItemId(id))
      throw new ApiException(HttpStatus.CONFLICT, "เมนูมีประวัติออเดอร์แล้ว ให้ปิดขายแทนการลบ");
    images.findById(id).ifPresent(images::delete);
    images.flush();
    menus.delete(m);
  }

  private ApiException missing() {
    return new ApiException(HttpStatus.NOT_FOUND, "ไม่พบเมนู");
  }
}
