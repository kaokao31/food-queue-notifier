package com.kku.queuenotify.service.impl;
import com.kku.queuenotify.dto.response.NotificationLogResponse;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.NotificationLogRepository;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.NotificationLogService;
import com.kku.queuenotify.service.OrderAccessService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class NotificationLogServiceImpl implements NotificationLogService {
  private final ObjectProvider<OrderAccessService> access;
  private final QueueRepository queues;
  private final NotificationLogRepository logs;
  public NotificationLogServiceImpl(ObjectProvider<OrderAccessService> access,QueueRepository queues,NotificationLogRepository logs) {
    this.access=access;this.queues=queues;this.logs=logs;
  }
  @Override public List<NotificationLogResponse> forStaff(Long id) {
    var provider=access.getIfAvailable();
    if(provider==null) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Staff access service is not available");
    if(!provider.isStaff()) throw new ApiException(HttpStatus.FORBIDDEN,"Staff access is required");
    if(id==null || id<=0) throw new ApiException(HttpStatus.BAD_REQUEST,"Order ID must be positive");
    if(!queues.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND,"Order not found");
    return logs.findByQueueIdOrderByIdDesc(id).stream().map(log->{
      String status=List.of("PENDING","PREVIEW","ACCEPTED","FAILED").contains(log.getDeliveryStatus()==null?"":log.getDeliveryStatus())?log.getDeliveryStatus():"LEGACY";
      String message=switch(status) {
        case "PENDING"->"การส่งยังไม่มีผลยืนยัน ไม่มีการลองซ้ำอัตโนมัติ";
        case "PREVIEW"->"ตัวอย่างในระบบ ไม่ได้ส่งไปยังผู้ให้บริการ";
        case "ACCEPTED"->"ผู้ให้บริการรับคำขอแล้ว ยังไม่ยืนยันว่าอุปกรณ์แสดงแจ้งเตือน";
        case "FAILED"->"การส่งไม่สำเร็จ ไม่มีการลองซ้ำอัตโนมัติ";
        default->"ข้อมูลแจ้งเตือนเดิม ไม่ได้ยืนยันการส่งในระบบปัจจุบัน";
      };
      Integer http=log.getHttpStatus();if(http!=null && (http<100 || http>599)) http=null;
      return new NotificationLogResponse(log.getId(),log.getChannel()==null?"UNKNOWN":log.getChannel().name(),
          "READY".equals(log.getEventType())?"READY":"LEGACY",status,http,log.getAttemptedAt(),log.getSentAt(),message);
    }).toList();
  }
}
