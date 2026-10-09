package com.kku.queuenotify.controller.api;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
  @GetMapping("/api/v1/csrf")
  public ResponseEntity<Map<String,String>> token(CsrfToken token) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
        "headerName",token.getHeaderName(),"parameterName",token.getParameterName(),"token",token.getToken()));
  }
}
