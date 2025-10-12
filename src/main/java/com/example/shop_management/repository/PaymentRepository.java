package com.example.shop_management.repository;

import com.example.shop_management.model.Installment;
import com.example.shop_management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByOrderhistory_User_IdOrderByIdDesc(Long userId);


    Optional<Payment> findByOrderhistory_Id(Long orderId);

    // Tìm payment theo id của OrderHistory
    @Query("SELECT p FROM Payment p WHERE p.orderhistory.id = :orderId")
    Payment findByOrderhistoryId(Long orderId);

}
