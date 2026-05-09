package com.cloudcart.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "order");
    }

    @PostMapping("/api/order/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> payload) {

        // Call payment-mock service (port 8080 inside cluster)
        Map payment = restTemplate.postForObject(
            "http://payment-mock:8080/pay",
            payload,
            Map.class
        );

        // Call notification service (port 8080 inside cluster)
        Map notification = restTemplate.postForObject(
            "http://notification:8080/send",
            Map.of("channel", "email", "message", "order created"),
            Map.class
        );

        String orderStatus = "success".equals(payment.get("status"))
            ? "created"
            : "payment_failed";

        return Map.of(
            "order_status",  orderStatus,
            "payment",       payment,
            "notification",  notification
        );
    }
}
