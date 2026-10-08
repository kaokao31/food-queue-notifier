package com.kku.queuenotify.common;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QueueToken {
  private final SecureRandom random = new SecureRandom();

  public String generate() {
    byte[] b = new byte[32];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public boolean matches(String raw, String hash) {
    return raw != null
        && hash != null
        && MessageDigest.isEqual(
            hash(raw).getBytes(StandardCharsets.US_ASCII),
            hash.getBytes(StandardCharsets.US_ASCII));
  }
}
