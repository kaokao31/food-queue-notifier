package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.MenuItemResponse;
import com.kku.queuenotify.service.MenuImageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/menu-items")
@Tag(name = "Menu")
public class MenuImageController {
  private final MenuImageService images;

  public MenuImageController(MenuImageService images) {
    this.images = images;
  }

  @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MenuItemResponse> create(
      @Valid @RequestPart("menu") MenuItemRequest request,
      @RequestPart("file") MultipartFile file) {
    var result = images.saveMenu(null, request, file);
    return ResponseEntity.created(URI.create("/api/v1/menu-items/" + result.id())).body(result);
  }

  @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public MenuItemResponse update(
      @PathVariable Long id,
      @Valid @RequestPart("menu") MenuItemRequest request,
      @RequestPart("file") MultipartFile file) {
    return images.saveMenu(id, request, file);
  }

  @GetMapping("/{id}/image")
  public ResponseEntity<byte[]> get(@PathVariable Long id) {
    var image = images.get(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
        .eTag(image.key())
        .body(image.data());
  }
}
