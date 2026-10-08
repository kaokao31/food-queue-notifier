package com.kku.queuenotify.service;

/** Contract for the token implementation supplied by the access/security module. */
public interface QueueTokenGenerator {
  String generate();

  String hash(String token);
}
