package com.cloudcart.notification;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class NotificationController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "notification");
    }

    @PostMapping("/send")
    public Map<String, String> send(@RequestBody Map<String, Object> data) {
        return Map.of(
            "status",  "sent",
            "channel", (String) data.getOrDefault("channel", "email"),
            "message", "notification delivered"
        );
    }
}
