package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.common.MenuImageFiles;
import com.kku.queuenotify.domain.entity.MenuImage;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.mapper.MenuItemMapper;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class MenuImageServiceImpl implements MenuImageService {
  private final MenuService menus;
  private final MenuItemRepository items;
  private final MenuImageRepository images;
  private final MenuItemMapper mapper;

  public MenuImageServiceImpl(
      MenuService menus,
      MenuItemRepository items,
      MenuImageRepository images,
      MenuItemMapper mapper) {
    this.menus = menus;
    this.items = items;
    this.images = images;
    this.mapper = mapper;
  }

  public MenuItemResponse saveMenu(Long id, MenuItemRequest request, MultipartFile file) {
    var content = MenuImageFiles.validate(file);
    var saved = id == null ? menus.create(request) : menus.update(id, request);
    var item = items.lockById(saved.id()).orElseThrow(() -> missing());
    var image = images.findById(item.getId()).orElseGet(MenuImage::new);
    image.setMenuItem(item);
    image.setData(content.data());
    image.setContentType(content.contentType());
    images.save(image);
    item.setImageKey(content.key());
    return mapper.response(item);
  }

  @Transactional(readOnly = true)
  public MenuImageContent get(Long id) {
    var image = images.findById(id).orElseThrow(() -> missing());
    return new MenuImageContent(
        image.getData(), image.getContentType(), image.getMenuItem().getImageKey());
  }

  private ApiException missing() {
    return new ApiException(HttpStatus.NOT_FOUND, "ไม่พบภาพเมนู");
  }
}
