package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.exception.GlobalExceptionHandler;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queues")
public class QueueController {
  private final QueueService queues;
  public QueueController(QueueService queues){this.queues=queues;}

  @GetMapping("/{id}")
  public ResponseEntity<QueueResponse> get(@PathVariable Long id,
      @RequestHeader(value="X-Queue-Token",required=false) String token){
    return response(queues.get(id,token));
  }
  @PatchMapping("/{id}/advance")
  public ResponseEntity<QueueResponse> advance(@PathVariable Long id){
    return response(queues.advance(id));
  }
  @PatchMapping("/{id}/cancel")
  public ResponseEntity<QueueResponse> cancel(@PathVariable Long id,
      @RequestHeader(value="X-Queue-Token",required=false) String token){
    return response(queues.cancel(id,token));
  }
  private ResponseEntity<QueueResponse> response(QueueResponse queue){
    return ResponseEntity.ok().header("Cache-Control","no-store").body(queue);
  }
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,Object>> error(ApiException error){
    return GlobalExceptionHandler.response(error.getStatus(),error.getMessage());
  }
}
