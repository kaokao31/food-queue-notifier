package com.kku.queuenotify.exception;

import com.kku.queuenotify.controller.api.PushController;
import com.kku.queuenotify.controller.api.OrderSubscriptionController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles only the Push controllers; other module errors remain separate. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {PushController.class, OrderSubscriptionController.class})
public class PushConfigurationExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
    return handle(new PushDemoException(ex.getStatus(), ex.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
    return handle(new PushDemoException(HttpStatus.CONFLICT, "Subscription changed concurrently; please refresh"));
  }

  @ExceptionHandler(PushDemoException.class)
  public ResponseEntity<Map<String, Object>> handle(PushDemoException ex) {
    return ResponseEntity.status(ex.getStatus()).cacheControl(CacheControl.noStore())
        .body(Map.of("timestamp", Instant.now().toString(), "status", ex.getStatus().value(),
            "error", ex.getStatus().getReasonPhrase(), "message", ex.getMessage()));
  }
}
