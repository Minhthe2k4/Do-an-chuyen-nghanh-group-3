package com.example.shop_management.repository;

import com.example.shop_management.model.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {



        @Query("SELECT i FROM Installment i WHERE i.payment.orderhistory.user.id = :userId")
        List<Installment> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i " +
            "JOIN i.payment p " +
            "JOIN p.orderhistory o " +
            "JOIN o.user u " +
            "WHERE u.id = :userId AND i.paid = false")
    Long findTotalUnpaidAmountByUserId(@Param("userId") Long userId);



}



