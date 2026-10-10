package com.kku.queuenotify.support;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
/** Test-only providers isolate A ordering persistence before C implements real security. */
@TestConfiguration(proxyBeanMethods=false)
public class OrderingTestDoubles {
 @Bean @Primary public Tokens fixtureTokens(){return new Tokens();}
 @Bean @Primary public Access fixtureAccess(QueueRepository queues,Tokens tokens){return new Access(queues,tokens);}
 public static class Tokens implements QueueTokenGenerator {
  private final AtomicLong sequence=new AtomicLong();public boolean failHash;
  public String generate(){return "ordering-test-token-"+sequence.incrementAndGet();}
  public String hash(String token){if(failHash)throw new IllegalStateException("test token provider failure");try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 }
 public static class Access implements OrderAccessService {
  private final QueueRepository queues;private final Tokens tokens;public boolean staff;
  public Access(QueueRepository queues,Tokens tokens){this.queues=queues;this.tokens=tokens;}
  public boolean isStaff(){return staff;}
  public Queue locked(Long id,String token){var queue=queues.lockById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"missing"));if(!staff&&(token==null||!tokens.hash(token).equals(queue.getTokenHash())))throw new ApiException(HttpStatus.FORBIDDEN,"test access denied");return queue;}
 }
}
