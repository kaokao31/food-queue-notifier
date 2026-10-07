package com.kku.queuenotify.service;

import com.kku.queuenotify.domain.entity.Customer;

/**
 * Strategy Pattern
 * ISP: interface นี้มีหน้าที่เดียวคือ "ส่งข้อความ" ไม่ปนกับการบันทึก log
 * (การบันทึก log แยกไปที่ NotificationLogWriter คนละ interface)
 */
public interface NotificationStrategy {

    NotificationResult send(Customer customer, String message);

    record NotificationResult(boolean success, String detail) {
    }
}
