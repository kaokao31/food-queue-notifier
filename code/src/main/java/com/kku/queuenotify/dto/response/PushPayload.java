package com.kku.queuenotify.dto.response;

/** Browser payload with a local, credential-free navigation destination. */
public record PushPayload(String title, String body, String tag, String url) {
  public PushPayload {
    if (title == null || title.isBlank() || body == null || tag == null || tag.isBlank()) {
      throw new IllegalArgumentException("Notification text and tag are required");
    }
    if (url == null || !(url.equals("/push-demo.html") || url.matches("/queue/[1-9][0-9]{0,18}"))) {
      throw new IllegalArgumentException("Notification destination must be a local queue or demo page");
    }
  }

  public static PushPayload ready(Long queueId, String body) {
    if (queueId == null || queueId <= 0) throw new IllegalArgumentException("Queue ID must be positive");
    return new PushPayload("อาหารพร้อมแล้ว", body, "queue-" + queueId + "-ready", "/queue/" + queueId);
  }
}
