package com.example.eventnotification.controller;

import com.example.eventnotification.model.Order;
import com.example.eventnotification.model.OrderRequest;
import com.example.eventnotification.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getOrderId());
        body.put("message", "Order created and event published successfully");
        body.put("status", order.getStatus());
        return ResponseEntity.ok(body);
    }
}
