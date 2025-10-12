package com.example.shop_management.model;

import com.example.shop_management.Enum.PaymentMethod;
import com.example.shop_management.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethod payment_method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime paid_at;
    private LocalDateTime created_at;


    @PrePersist
    protected void onCreate() {
        created_at = LocalDateTime.now();
        paid_at = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        paid_at = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name="order_history_id")
    private OrderHistory orderhistory;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transaction;

    @OneToMany(mappedBy="payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Installment> installment;

}
