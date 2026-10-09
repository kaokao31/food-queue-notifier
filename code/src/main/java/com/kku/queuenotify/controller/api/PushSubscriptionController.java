package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.*;
import com.kku.queuenotify.service.PushSubscriptionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("push-demo") @RequestMapping("/api/v1/push-demo")
public class PushSubscriptionController {
  private final PushSubscriptionService subscriptions;
  public PushSubscriptionController(PushSubscriptionService subscriptions){this.subscriptions=subscriptions;}
  @PostMapping("/subscription") public ResponseEntity<Void> register(HttpSession session,@Valid @RequestBody PushSubscriptionRequest request){
    subscriptions.register(session.getId(),request);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
  @DeleteMapping("/subscription") public ResponseEntity<Void> remove(HttpSession session){
    subscriptions.remove(session.getId());return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
  @PostMapping("/send") public ResponseEntity<Map<String,Object>> send(HttpSession session){
    int status=subscriptions.send(session.getId());
    if(status<200 || status>=300)throw new PushDemoException(HttpStatus.BAD_GATEWAY,"Push provider did not accept the demo request");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("providerStatus",status,
        "message","Push provider accepted the request; device display is unconfirmed"));
  }
  @ExceptionHandler(PushDemoException.class) public ResponseEntity<Map<String,String>> demoError(PushDemoException error){
    return ResponseEntity.status(error.getStatus()).cacheControl(CacheControl.noStore()).body(Map.of("message",error.getMessage()));
  }
  @ExceptionHandler(ApiException.class) public ResponseEntity<Map<String,String>> validationError(ApiException error){
    return ResponseEntity.status(error.getStatus()).cacheControl(CacheControl.noStore()).body(Map.of("message",error.getMessage()));
  }
}
