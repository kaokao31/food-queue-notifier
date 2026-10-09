package com.kku.queuenotify.common;

import com.kku.queuenotify.service.QueueTokenGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Generates opaque owner tokens; persist the SHA-256 hash, never the raw token. */
@Component
public class QueueToken implements QueueTokenGenerator {
  private final SecureRandom random = new SecureRandom();

  @Override public String generate() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  @Override public String hash(String token) {
    if (token == null || token.isBlank() || token.length() > 256) {
      throw new IllegalArgumentException("Queue token is invalid");
    }
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available");
    }
  }

  public boolean matches(String raw, String storedHash) {
    if (raw == null || raw.isBlank() || raw.length() > 256
        || storedHash == null || !storedHash.matches("[0-9a-f]{64}")) return false;
    return MessageDigest.isEqual(HexFormat.of().parseHex(hash(raw)), HexFormat.of().parseHex(storedHash));
  }
}
