package com.kku.queuenotify.controller.api;
import com.kku.queuenotify.exception.GlobalExceptionHandler;
import com.kku.queuenotify.dto.response.NotificationLogResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.NotificationLogService;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders/{id}/notifications")
public class NotificationLogController {
  private final NotificationLogService logs;
  public NotificationLogController(NotificationLogService logs) {this.logs=logs;}
  @GetMapping public ResponseEntity<List<NotificationLogResponse>> list(@PathVariable Long id) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(logs.forStaff(id));
  }
  // Preserve domain status and message using the shared error contract.
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,Object>> error(ApiException error) {
    return GlobalExceptionHandler.response(error.getStatus(),error.getMessage());
  }
}
