package com.kku.queuenotify.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PushSubscriptionRequest(
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https://\\S+$")
        String endpoint,

        @NotNull
        @Valid
        Keys keys
) {
    public record Keys(
            @NotBlank @Size(max = 256)
            String p256dh,

            @NotBlank @Size(max = 256)
            String auth
    ) {
    }
}