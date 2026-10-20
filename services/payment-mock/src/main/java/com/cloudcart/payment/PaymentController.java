package com.cloudcart.payment;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

@RestController
public class PaymentController {

    private final String[] outcomes = {"success", "success", "success", "failed"};
    private final Random random = new Random();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "payment-mock");
    }

    @PostMapping("/pay")
    public Map<String, Object> pay(@RequestBody Map<String, Object> data) {
        String result = outcomes[random.nextInt(outcomes.length)];
        int txnId = 1000 + random.nextInt(9000);

        return Map.of(
            "status",         result,
            "amount",         data.getOrDefault("amount", 0),
            "transaction_id", "txn-" + txnId
        );
    }
}
