package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.service.impl.QueueNumberServiceImpl;
import java.time.*;
import org.junit.jupiter.api.Test;
class QueueNumberServiceTest {
 @Test void selectsBangkokDayAcrossMidnightRegardlessOfClockZone(){
  for(var instant:new String[]{"2026-10-09T16:59:59Z","2026-10-09T17:00:00Z"}){
   var expected=LocalDate.of(2026,10,instant.contains("16:")?9:10);
   // Counter fixture returns an existing high number; it is not a production allocator.
   var service=new QueueNumberServiceImpl(date->{assertEquals(expected,date);return 1000;},Clock.fixed(Instant.parse(instant),ZoneId.of("America/New_York")));
   var number=service.next();assertEquals(expected,number.date());assertEquals(1000,number.value());
  }
 }
}
