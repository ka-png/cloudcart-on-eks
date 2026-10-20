package com.cloudcart.cart;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class CartController {

    private final List<Map<String, Object>> cart = new ArrayList<>();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "cart");
    }

    @GetMapping("/api/cart/items")
    public List<Map<String, Object>> getItems() {
        return cart;
    }

    @PostMapping("/api/cart/items")
    public Map<String, Object> addItem(@RequestBody Map<String, Object> item) {
        cart.add(item);
        return Map.of("message", "item added", "cart", cart);
    }

    @DeleteMapping("/api/cart/items")
    public Map<String, String> clearCart() {
        cart.clear();
        return Map.of("message", "cart cleared");
    }
}
