package com.kku.queuenotify.controller;
import com.kku.queuenotify.service.QrService;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.exception.ApiException;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller
public class QrController {
 private final QrService qr;private final ObjectProvider<OrderAccessService> access;
 public QrController(QrService qr,ObjectProvider<OrderAccessService> access){this.qr=qr;this.access=access;}
 private void requireStaff(){var provider=access.getIfAvailable();if(provider==null)throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ระบบสิทธิ์พนักงานยังไม่พร้อมใช้งาน");if(!provider.isStaff())throw new ApiException(HttpStatus.FORBIDDEN,"สำหรับพนักงานเท่านั้น");}
 @GetMapping("/staff/qr") public String page(){requireStaff();return "qr";}
 @GetMapping(value="/staff/qr.png",produces=MediaType.IMAGE_PNG_VALUE)
 @ResponseBody public ResponseEntity<byte[]> image(@RequestParam String url) throws Exception {requireStaff();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(qr.generateMenuQr(url));}
 @ExceptionHandler(ApiException.class) public ResponseEntity<Map<String,String>> error(ApiException error){return ResponseEntity.status(error.getStatus()).body(Map.of("message",error.getMessage()));}
}
