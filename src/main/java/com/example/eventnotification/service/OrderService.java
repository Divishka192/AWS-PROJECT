package com.example.eventnotification.service;

import com.example.eventnotification.model.Order;
import com.example.eventnotification.model.OrderEvent;
import com.example.eventnotification.model.OrderRequest;
import com.example.eventnotification.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final SnsPublisherService snsPublisherService;

    public OrderService(OrderRepository orderRepository, SnsPublisherService snsPublisherService) {
        this.orderRepository = orderRepository;
        this.snsPublisherService = snsPublisherService;
    }

    /** Saves the order in PostgreSQL and returns it with its ORD#### id. */
    public Order saveOrder(OrderRequest request) {
        Order order = new Order();
        order.setOrderId("TEMP-" + UUID.randomUUID()); // replaced right after we know the DB id
        order.setCustomer(request.getCustomer());
        order.setProduct(request.getProduct());
        order.setAmount(request.getAmount());
        order.setStatus("ORDER_CREATED");
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.save(order);
        order.setOrderId("ORD" + (1000 + order.getId()));   // ORD1001, ORD1002, ...
        return orderRepository.save(order);
    }

    /** Full flow: save to DB, then publish one event to SNS. */
    public Order createOrder(OrderRequest request) {
        Order order = saveOrder(request);

        OrderEvent event = new OrderEvent(order.getOrderId(), order.getCustomer(),
                order.getProduct(), order.getAmount(), "ORDER_CREATED");

        try {
            snsPublisherService.publish(event);
        } catch (EventPublishException e) {
            order.setStatus("EVENT_PUBLISH_FAILED");
            orderRepository.save(order);
            log.error("Order {} saved but event publish failed", order.getOrderId());
            throw e;
        }
        return order;
    }
}
