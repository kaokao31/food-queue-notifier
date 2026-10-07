package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class BrowserWebPushSenderTest {
    @Test
    void senderIsExecutableAndReportsMissingConfigurationWithoutSending() {
        var sender = new BrowserWebPushSender(new WebPushProperties());
        var subscription = new PushSubscriptionRequest(
                "https://fcm.googleapis.com/wp/test",
                new PushSubscriptionRequest.Keys("unused", "unused"));
        var error = assertThrows(PushDemoException.class,
                () -> sender.sendTest(subscription));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }
}
