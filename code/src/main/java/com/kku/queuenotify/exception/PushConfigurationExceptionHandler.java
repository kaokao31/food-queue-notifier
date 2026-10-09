package com.kku.queuenotify.exception;

import com.kku.queuenotify.controller.api.PushController;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles only the Push configuration controller; other module errors remain separate. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PushController.class)
public class PushConfigurationExceptionHandler {
  @ExceptionHandler(PushDemoException.class)
  public ResponseEntity<Map<String, Object>> handle(PushDemoException ex) {
    return ResponseEntity.status(ex.getStatus()).cacheControl(CacheControl.noStore())
        .body(Map.of("timestamp", Instant.now().toString(), "status", ex.getStatus().value(),
            "error", ex.getStatus().getReasonPhrase(), "message", ex.getMessage()));
  }
}
