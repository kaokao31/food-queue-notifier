package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.service.PushConfigurationService;
import org.springframework.stereotype.Service;

@Service
public class PushConfigurationServiceImpl
        implements PushConfigurationService {

    private final WebPushProperties properties;

    public PushConfigurationServiceImpl(WebPushProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getPublicKey() {
        String key = properties.getPublicKey();

        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Web Push public key is not configured"
            );
        }

        return key;
    }
}