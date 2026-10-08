package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

public interface MenuImageService {
  MenuItemResponse saveMenu(Long id, MenuItemRequest request, MultipartFile file);

  MenuImageContent get(Long id);
}
