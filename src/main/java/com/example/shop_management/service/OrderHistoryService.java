package com.example.shop_management.service;

import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.repository.OrderHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderHistoryService {

    private OrderHistoryRepository orderHistoryRepository;

    public OrderHistoryService (OrderHistoryRepository orderHistoryRepository) {
        this.orderHistoryRepository = orderHistoryRepository;
    }

    public OrderHistory getOrderHistoryById(Long id) {
        return orderHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with Id" + id));
    }

    public OrderHistory updateOrderHistory(Long id, OrderHistory orderHistory) {
        OrderHistory existingOrderHistory = getOrderHistoryById(id);
        existingOrderHistory.setId(orderHistory.getId());
        existingOrderHistory.setShipping_status(orderHistory.getShipping_status());
        existingOrderHistory.setUpdated_at(java.time.LocalDateTime.now());
        existingOrderHistory.setCreated_at(java.time.LocalDateTime.now());
        existingOrderHistory.setStatus(orderHistory.getStatus());
        return orderHistoryRepository.save(existingOrderHistory);
    }
}
