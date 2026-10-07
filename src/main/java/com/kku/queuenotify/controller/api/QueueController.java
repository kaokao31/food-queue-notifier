package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.mapper.QueueMapper;
import com.kku.queuenotify.service.impl.QueueServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Presentation Layer เท่านั้น — ห้ามเรียก Repository ตรง ทุกอย่างผ่าน Service
@Tag(name = "Queue")
@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueServiceImpl queueService;
    private final QueueMapper queueMapper;

    @Operation(summary = "เลื่อนคิวไปสถานะถัดไป (State Pattern + Observer แจ้งเตือนลูกค้า)")
    @PatchMapping("/{id}/advance")
    public ResponseEntity<QueueResponse> advance(@PathVariable Long id) {
        Queue queue = queueService.advanceQueue(id);
        return ResponseEntity.ok(queueMapper.toResponse(queue));
    }

    @Operation(summary = "ยกเลิกคิว (ทำได้เฉพาะ WAITING/PREPARING)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<QueueResponse> cancel(@PathVariable Long id) {
        Queue queue = queueService.cancelQueue(id);
        return ResponseEntity.ok(queueMapper.toResponse(queue));
    }
}
