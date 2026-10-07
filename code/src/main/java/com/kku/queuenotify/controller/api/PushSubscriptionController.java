package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.PushSubscriptionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@Profile("push-demo")
@RequestMapping("/api/v1/push/subscriptions")
public class PushSubscriptionController {
    private final PushSubscriptionService subscriptionService;

    public PushSubscriptionController(PushSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody PushSubscriptionRequest request, HttpSession session) {
        subscriptionService.subscribe(session.getId(), request);
        return ResponseEntity.noContent().build();
    }

    // A custom header prevents cross-origin HTML forms from triggering sends.
    // No cross-origin access is enabled on these demo endpoints.
    @PostMapping(value = "/test", headers = "X-Push-Demo=true")
    public Map<String, String> sendTest(HttpSession session) {
        subscriptionService.sendTest(session.getId());
        return Map.of("message", "บริการ Push รับคำขอแล้ว กรุณาตรวจแจ้งเตือนของเครื่องนี้ (ยังไม่ยืนยันการแสดงผล)");
    }

    @ExceptionHandler(PushDemoException.class)
    public ResponseEntity<Map<String, String>> handleDemoError(PushDemoException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }
}
