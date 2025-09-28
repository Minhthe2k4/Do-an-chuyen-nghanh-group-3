package com.example.shop_management.repository;


import com.example.shop_management.DTO.OrderHistoryDTO;
import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    @Query("SELECT new com.example.shop_management.DTO.OrderHistoryDTO(u.username, or.order_items, or.status, or.total_amount) FROM OrderHistory or JOIN or.user u")
    List<OrderHistoryDTO> getProduct();

    Optional<OrderHistory> findTopByUser_IdOrderByIdDesc(Long userId);

    List<OrderHistory> findByUserId(Long userId);
}
