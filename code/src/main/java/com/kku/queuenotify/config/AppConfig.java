package com.kku.queuenotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync // จำเป็นสำหรับ @Async บน NotificationEventListener
public class AppConfig {

    @Bean // Singleton Pattern โดย Spring container จัดการให้อัตโนมัติ
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
