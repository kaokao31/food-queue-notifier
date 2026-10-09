package com.kku.queuenotify.service;

public interface PushConfigurationService {
  /** Return a validated public key for browser subscription, never a private key. */
  String getPublicKey();
}
