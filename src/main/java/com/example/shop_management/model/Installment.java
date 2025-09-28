package com.example.shop_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="installments")
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long installment_no;
    private BigDecimal amount;
    private BigDecimal late_fee;
    private LocalDateTime due_date;
    private boolean paid;
    private LocalDateTime created_at;
    private LocalDateTime paid_at;

    @PrePersist
    protected void onCreate() {
        created_at = LocalDateTime.now();
        paid_at = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "installment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transaction;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;



}
