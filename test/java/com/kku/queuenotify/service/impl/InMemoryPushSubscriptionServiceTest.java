package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.WebPushSender;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InMemoryPushSubscriptionServiceTest {
    private final WebPushSender sender = mock(WebPushSender.class);
    private final InMemoryPushSubscriptionService service =
            new InMemoryPushSubscriptionService(sender);

    private PushSubscriptionRequest subscription(String endpoint) {
        byte[] key = new byte[65];
        key[0] = 4;
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return new PushSubscriptionRequest(endpoint,
                new PushSubscriptionRequest.Keys(encoder.encodeToString(key),
                        encoder.encodeToString(new byte[16])));
    }

    @Test
    void sendsOnlyToTheCurrentSession() {
        var first = subscription("https://fcm.googleapis.com/fcm/send/first");
        var second = subscription("https://fcm.googleapis.com/wp/second");
        service.subscribe("first-session", first);
        service.subscribe("second-session", second);
        when(sender.sendTest(first)).thenReturn(201);
        service.sendTest("first-session");
        verify(sender).sendTest(first);
        verifyNoMoreInteractions(sender);
    }

    @Test
    void unregisteredSessionCannotSend() {
        service.subscribe("registered", subscription("https://fcm.googleapis.com/wp/a"));
        var error = assertThrows(PushDemoException.class, () -> service.sendTest("other"));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verifyNoInteractions(sender);
    }

    @Test
    void rejectsArbitraryAndSpoofedEndpoints() {
        for (String endpoint : new String[] {
                "https://127.0.0.1/internal", "http://fcm.googleapis.com/wp/a",
                "https://fcm.googleapis.com.evil.test/wp/a",
                "https://fcm.googleapis.com@evil.test/wp/a",
                "https://fcm.googleapis.com:8443/wp/a",
                "https://fcm.googleapis.com/unrelated"}) {
            assertThrows(PushDemoException.class,
                    () -> service.subscribe("session", subscription(endpoint)));
        }
        verifyNoInteractions(sender);
    }

    @Test
    void rejectsInvalidEncryptionKeys() {
        var request = new PushSubscriptionRequest("https://fcm.googleapis.com/wp/a",
                new PushSubscriptionRequest.Keys("bad", "bad"));
        assertThrows(PushDemoException.class, () -> service.subscribe("session", request));
        verifyNoInteractions(sender);
    }

    @Test
    void expiredSubscriptionIsRemoved() {
        var request = subscription("https://fcm.googleapis.com/wp/a");
        service.subscribe("session", request);
        when(sender.sendTest(request)).thenReturn(410);
        assertEquals(HttpStatus.GONE,
                assertThrows(PushDemoException.class, () -> service.sendTest("session")).getStatus());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(PushDemoException.class, () -> service.sendTest("session")).getStatus());
        verify(sender, times(1)).sendTest(request);
    }

    @Test
    void resubscribingDoesNotBypassSendCooldown() {
        var request = subscription("https://fcm.googleapis.com/wp/a");
        service.subscribe("session", request);
        when(sender.sendTest(request)).thenReturn(201);
        service.sendTest("session");
        service.subscribe("session", request);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(PushDemoException.class, () -> service.sendTest("session")).getStatus());
        verify(sender, times(1)).sendTest(request);
    }

    @Test
    void upstreamRejectionIsNotReportedAsSuccess() {
        var request = subscription("https://fcm.googleapis.com/wp/a");
        service.subscribe("session", request);
        when(sender.sendTest(request)).thenReturn(403);
        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(PushDemoException.class, () -> service.sendTest("session")).getStatus());
    }
}
