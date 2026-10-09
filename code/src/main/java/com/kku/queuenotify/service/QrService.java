package com.kku.queuenotify.service;

/** Generates a PNG QR for a validated public HTTPS menu URL. */
public interface QrService {
  byte[] generateMenuQr(String url) throws Exception;
}
