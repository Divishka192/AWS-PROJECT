package com.example.eventnotification.repository;

import com.example.eventnotification.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
