package com.kku.queuenotify.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
  @GetMapping("/")
  public String menu() {
    return "menu";
  }

  @GetMapping("/queue/{id}")
  public String queue() {
    return "queue";
  }

  @GetMapping("/staff/login")
  public String login() {
    return "login";
  }

  @GetMapping("/staff")
  public String staff() {
    return "staff";
  }

  @GetMapping("/staff/menu")
  public String staffMenu() {
    return "staff-menu";
  }
}
