package com.kku.queuenotify.mapper;

import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.common.MenuPhotos;
import com.kku.queuenotify.dto.response.MenuItemResponse;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {
  public MenuItemResponse response(MenuItem m) {
    return new MenuItemResponse(
        m.getId(),
        m.getName(),
        m.getCategory(),
        m.getPrice(),
        m.getPrepTimeMinutes(),
        m.isAvailable(),
        MenuPhotos.imageUrl(m.getId(), m.getImageKey()));
  }
}
