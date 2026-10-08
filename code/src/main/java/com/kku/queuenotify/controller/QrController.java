package com.kku.queuenotify.controller;

import com.kku.queuenotify.service.QrService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/** Presents the staff QR page and delegates PNG generation to its service. */
@Controller
public class QrController {
  private final QrService qrService;

  public QrController(QrService qrService) {
    this.qrService = qrService;
  }

  @GetMapping("/staff/qr")
  public String page() {
    return "qr";
  }

  @GetMapping(value = "/staff/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  @ResponseBody
  public ResponseEntity<byte[]> image(@RequestParam String url) throws Exception {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(qrService.generateMenuQr(url));
  }
}
