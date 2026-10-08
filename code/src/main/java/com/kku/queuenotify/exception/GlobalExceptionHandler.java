package com.kku.queuenotify.exception;

import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateTransitionException.class)
  public ResponseEntity<Object> handleIllegalState(IllegalStateTransitionException ex) {
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
    return buildResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Object> api(ApiException ex) {
    return buildResponse(ex.getStatus(), ex.getMessage());
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ResponseEntity<Object> resource(Exception ex) {
    return buildResponse(HttpStatus.NOT_FOUND, "ไม่พบหน้าที่ร้องขอ");
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  public ResponseEntity<Object> badInput(Exception ex) {
    return buildResponse(HttpStatus.BAD_REQUEST, "ข้อมูลคำขอไม่ถูกต้อง");
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<Object> conflict(Exception ex) {
    return buildResponse(HttpStatus.CONFLICT, "ข้อมูลขัดแย้ง กรุณารีเฟรชแล้วลองใหม่");
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<Object> largeUpload(Exception ex) {
    return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "รูปภาพต้องมีขนาดไม่เกิน 2 MB");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneric(Exception ex) {
    // Log types and code locations only: exception messages may contain secrets.
    Throwable cause = ex;
    for (int depth = 0; cause != null && depth < 8; depth++) {
      log.error("Unhandled error cause[{}]: {}", depth, cause.getClass().getName());
      for (StackTraceElement frame : cause.getStackTrace()) {
        log.error("  at {}", frame);
      }
      if (cause.getCause() == cause) {
        break;
      }
      cause = cause.getCause();
    }
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดที่ไม่คาดคิด");
  }

  private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
    Map<String, Object> body =
        Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message);
    return ResponseEntity.status(status).body(body);
  }
}
