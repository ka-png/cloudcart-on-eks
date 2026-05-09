package com.cloudcart.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CatalogController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "catalog");
    }

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("message", "Catalog service is running");
    }

    @GetMapping("/api/catalog/products")
    public List<Map<String, Object>> products() {
        return List.of(
            Map.of("id", 1, "name", "Wireless Headphones", "price", 4999),
            Map.of("id", 2, "name", "Smart Watch",         "price", 2999),
            Map.of("id", 3, "name", "Bluetooth Speaker",   "price", 1999),
            Map.of("id", 4, "name", "Gaming Mouse",        "price", 1499)
        );
    }
}
