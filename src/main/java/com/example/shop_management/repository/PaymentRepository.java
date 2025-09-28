package com.example.shop_management.repository;

import com.example.shop_management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByOrderhistory_User_IdOrderByIdDesc(Long userId);
    Optional<Payment> findByOrderhistory_Id(Long orderId);
}
