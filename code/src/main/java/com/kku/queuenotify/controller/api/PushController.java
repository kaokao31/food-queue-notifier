package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.response.PushPublicKeyResponse;
import com.kku.queuenotify.service.PushConfigurationService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push")
public class PushController {
  private final PushConfigurationService configurationService;

  public PushController(PushConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  @GetMapping("/public-key")
  public ResponseEntity<PushPublicKeyResponse> getPublicKey() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(new PushPublicKeyResponse(configurationService.getPublicKey()));
  }
}
