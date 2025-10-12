package com.example.shop_management.repository;


import com.example.shop_management.DTO.OrderHistoryDTO;
import com.example.shop_management.model.OrderHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    @Query("""
    SELECT new com.example.shop_management.DTO.OrderHistoryDTO(
        orh.id,
        u.username,
        orh.total_amount,
        a.addressLine,
        p.payment_method,
        orh.shipping_status,
        orh.status,
        orh.order_items
    )
    FROM OrderHistory orh
    JOIN orh.user u
    LEFT JOIN orh.payment p
    LEFT JOIN orh.address a
""")
    List<OrderHistoryDTO> getProduct();

    Optional<OrderHistory> findTopByUser_IdOrderByIdDesc(Long userId);

    @Query("SELECT oh FROM OrderHistory oh JOIN FETCH oh.payment WHERE oh.user.id = :userId")
    List<OrderHistory> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderHistory o WHERE o.id = :id AND o.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

}
