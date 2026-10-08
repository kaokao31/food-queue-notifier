package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import org.springframework.data.domain.Pageable;

public interface MenuService {
  PageResponse<MenuItemResponse> list(Pageable page, boolean all);

  MenuItemResponse get(Long id, boolean all);

  MenuItemResponse create(MenuItemRequest r);

  MenuItemResponse update(Long id, MenuItemRequest r);

  void delete(Long id);
}
