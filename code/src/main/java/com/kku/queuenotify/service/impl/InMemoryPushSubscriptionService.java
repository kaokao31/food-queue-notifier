package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.*;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service @Profile("push-demo")
public class InMemoryPushSubscriptionService implements PushSubscriptionService {
  private final Map<String,Entry> entries=new HashMap<>();
  private final SubscriptionValidator validator;
  private final WebPushSender sender;
  private final Clock clock;
  private static class Entry {
    final PushSubscriptionRequest request;final Instant expires;boolean sending;
    Entry(PushSubscriptionRequest request,Instant expires){this.request=request;this.expires=expires;}
  }
  public InMemoryPushSubscriptionService(SubscriptionValidator validator,WebPushSender sender,Clock clock){
    this.validator=validator;this.sender=sender;this.clock=clock;
  }
  private void clean(){entries.entrySet().removeIf(e->!e.getValue().sending && !e.getValue().expires.isAfter(clock.instant()));}
  private void idle(Entry entry){if(entry!=null && entry.sending)throw new PushDemoException(HttpStatus.CONFLICT,"Demo send is already in progress");}
  @Override public synchronized void register(String sessionId,PushSubscriptionRequest request){
    validator.validate(request);clean();idle(entries.get(sessionId));
    if(!entries.containsKey(sessionId) && entries.size()>=100)throw new PushDemoException(HttpStatus.TOO_MANY_REQUESTS,"Demo capacity reached; try later");
    entries.put(sessionId,new Entry(request,clock.instant().plus(Duration.ofMinutes(15))));
  }
  @Override public synchronized void remove(String sessionId){idle(entries.get(sessionId));entries.remove(sessionId);}
  @Override public int send(String sessionId){
    Entry entry;
    synchronized(this){clean();entry=entries.get(sessionId);
      if(entry==null)throw new PushDemoException(HttpStatus.NOT_FOUND,"Register a demo subscription first");
      idle(entry);entry.sending=true;
    }
    try {return sender.sendTest(entry.request);}
    catch(PushDemoException error){throw error;}
    catch(RuntimeException error){throw new PushDemoException(HttpStatus.BAD_GATEWAY,"Demo notification attempt failed");}
    finally {synchronized(this){entry.sending=false;}}
  }
}
