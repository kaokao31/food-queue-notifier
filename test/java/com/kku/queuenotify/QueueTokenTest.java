package com.kku.queuenotify;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.QueueTokenGenerator;
import com.kku.queuenotify.support.OrderingTestDoubles;
import java.util.Base64;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class QueueTokenTest {
  private final QueueToken tokens = new QueueToken();

  @Test void generatesDistinctUrlSafeTokensWith32RandomBytes() {
    var values=new HashSet<String>();
    for (int i=0;i<1000;i++) {
      String token=tokens.generate();assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
      assertEquals(32,Base64.getUrlDecoder().decode(token).length);assertTrue(values.add(token));
    }
  }
  @Test void hashingUsesDeterministicSha256WithoutReturningRawToken() {
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",tokens.hash("abc"));
    String raw=tokens.generate();String hash=tokens.hash(raw);
    assertEquals(hash,tokens.hash(raw));assertTrue(hash.matches("[0-9a-f]{64}"));assertNotEquals(raw,hash);
  }
  @Test void onlyMatchingTokenIsAccepted() {
    String first=tokens.generate();String second=tokens.generate();
    assertTrue(tokens.matches(first,tokens.hash(first)));
    assertFalse(tokens.matches(second,tokens.hash(first)));assertFalse(tokens.matches(first,first));
  }
  @Test void missingMalformedAndOversizedInputsFailClosed() {
    String hash=tokens.hash("fixture");
    for(String raw:new String[]{null,""," ","a".repeat(257)}) {
      assertFalse(tokens.matches(raw,hash));assertThrows(IllegalArgumentException.class,()->tokens.hash(raw));
    }
    for(String invalid:new String[]{null,"","a".repeat(63),"x".repeat(64),"a".repeat(65)})
      assertFalse(tokens.matches("fixture",invalid));
  }
  @Test void maximumInputSizeHasStableMatchingBehavior() {
    String raw="a".repeat(256);assertTrue(tokens.matches(raw,tokens.hash(raw)));
    assertFalse(tokens.matches(raw,"0".repeat(64)));
  }
  @Test void productionContextProvidesRealGeneratorWithoutTestFixtures() {
    new ApplicationContextRunner().withUserConfiguration(QueueToken.class).run(ctx->{
      assertNull(ctx.getStartupFailure());assertEquals(1,ctx.getBeansOfType(QueueTokenGenerator.class).size());
      assertInstanceOf(QueueToken.class,ctx.getBean(QueueTokenGenerator.class));
    });
  }
  @Test void existingOrderingTestsSelectPrimaryFixtureForRollbackInjection() {
    new ApplicationContextRunner().withUserConfiguration(QueueToken.class,OrderingTestDoubles.class)
        .withBean(QueueRepository.class,()->mock(QueueRepository.class)).run(ctx->{
          assertNull(ctx.getStartupFailure());
          var fixture=ctx.getBean(OrderingTestDoubles.Tokens.class);
          assertSame(fixture,ctx.getBeanProvider(QueueTokenGenerator.class).getIfAvailable());
          fixture.failHash=true;
          assertThrows(IllegalStateException.class,()->ctx.getBean(QueueTokenGenerator.class).hash("fixture"));
          assertNotNull(ctx.getBean(QueueToken.class).hash("fixture"));
        });
  }
  @Test void changingAnyTokenCharacterCannotMatchTheOriginalHash() {
    String raw="AbCdEf0123456789_-owner-fixture";String hash=tokens.hash(raw);
    for(int i=0;i<raw.length();i++){
      char replacement=raw.charAt(i)=='x'?'y':'x';
      String changed=raw.substring(0,i)+replacement+raw.substring(i+1);
      assertFalse(tokens.matches(changed,hash),"Changed token character "+i);
    }
    assertTrue(tokens.matches(raw,hash));
  }
  @Test void tokensAreNotTrimmedCaseFoldedOrAcceptedAsStoredHash() {
    String raw="CaseSensitive-owner-fixture";String hash=tokens.hash(raw);
    for(String changed:new String[]{raw.toLowerCase(java.util.Locale.ROOT)," "+raw,raw+" ",hash})
      assertFalse(tokens.matches(changed,hash));
    assertFalse(tokens.matches(raw,hash.toUpperCase(java.util.Locale.ROOT)));
    assertFalse(tokens.matches(raw,null));
  }
}
