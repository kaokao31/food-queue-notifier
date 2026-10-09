package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.exception.GlobalExceptionHandler;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.MenuItemResponse;
import com.kku.queuenotify.service.MenuImageService;
import com.kku.queuenotify.service.MenuService;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.exception.ApiException;
import org.springframework.beans.factory.ObjectProvider;
import java.util.Map;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/menu-items")
public class MenuImageController {
  private final MenuImageService images;
  private final MenuService menus;
  private final ObjectProvider<OrderAccessService> access;

  public MenuImageController(MenuImageService images, MenuService menus, ObjectProvider<OrderAccessService> access) {
    this.images = images;
    this.menus = menus;
    this.access = access;
  }

  @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MenuItemResponse> create(
      @Valid @RequestPart("menu") MenuItemRequest request,
      @RequestPart("file") MultipartFile file) {
    requireStaff();
    var result = images.saveMenu(null, request, file);
    return ResponseEntity.created(URI.create("/api/v1/menu-items/" + result.id())).body(result);
  }

  @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public MenuItemResponse update(
      @PathVariable Long id,
      @Valid @RequestPart("menu") MenuItemRequest request,
      @RequestPart("file") MultipartFile file) {
    requireStaff();
    return images.saveMenu(id, request, file);
  }

  @GetMapping("/{id}/image")
  public ResponseEntity<byte[]> get(@PathVariable Long id) {
    menus.get(id, isStaff());
    var image = images.get(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .cacheControl(CacheControl.noCache())
        .eTag(image.key())
        .body(image.data());
  }
  private boolean isStaff() {
    var service=access.getIfAvailable();return service!=null && service.isStaff();
  }
  private void requireStaff() {
    var service=access.getIfAvailable();
    if(service==null)throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ระบบสิทธิ์พนักงานยังไม่พร้อมใช้งาน");
    if(!service.isStaff())throw new ApiException(HttpStatus.FORBIDDEN,"สำหรับพนักงานเท่านั้น");
  }
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,Object>> domainError(ApiException error) {
    return GlobalExceptionHandler.response(error.getStatus(),error.getMessage());
  }
}
