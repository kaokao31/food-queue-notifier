package com.kku.queuenotify.exception;

import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** API error contract. Never serialize exception causes or rejected request values. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  public static Map<String,Object> body(HttpStatusCode status,String message) {
    HttpStatus known=HttpStatus.resolve(status.value());
    return Map.of("status",status.value(),"error",known==null?"Error":known.getReasonPhrase(),"message",message);
  }
  public static ResponseEntity<Map<String,Object>> response(HttpStatusCode status,String message) {
    return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body(status,message));
  }
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,Object>> business(ApiException ex){return response(ex.getStatus(),ex.getMessage());}
  @ExceptionHandler(PushDemoException.class)
  public ResponseEntity<Map<String,Object>> push(PushDemoException ex){return response(ex.getStatus(),ex.getMessage());}
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String,Object>> validation(ConstraintViolationException ex){return response(HttpStatus.BAD_REQUEST,"Invalid request");}
  @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class})
  public ResponseEntity<Map<String,Object>> conflict(RuntimeException ex){return response(HttpStatus.CONFLICT,"Data changed concurrently or conflicts with existing data");}
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String,Object>> denied(AccessDeniedException ex){return response(HttpStatus.FORBIDDEN,"Access denied");}
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String,Object>> unauthorized(AuthenticationException ex){return response(HttpStatus.UNAUTHORIZED,"Staff login is required");}
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String,Object>> unexpected(Exception ex){return response(HttpStatus.INTERNAL_SERVER_ERROR,"Internal server error");}
  @Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex,Object ignored,HttpHeaders headers,HttpStatusCode status,WebRequest request) {
    HttpHeaders safeHeaders=new HttpHeaders();safeHeaders.putAll(headers);safeHeaders.setCacheControl("no-store");
    String message=switch(status.value()) {
      case 400 -> "Invalid request";
      case 404 -> "Resource not found";
      case 405 -> "Method not allowed";
      case 406 -> "Requested response type is not supported";
      case 413 -> "Request is too large";
      case 415 -> "Unsupported content type";
      default -> status.is5xxServerError()?"Internal server error":"Request could not be processed";
    };
    return new ResponseEntity<>(body(status,message),safeHeaders,status);
  }
}
