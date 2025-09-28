package com.example.shop_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;
    private String bank_code;
    private String bank_tran_no;
    private String card_type;
    private String order_info;
    private String pay_date;
    private String response_code;
    private String tmn_code;
    private String transaction_no;
    private String transaction_status;
    private String txn_ref;
    private String secure_hash;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "installment_id")
    private Installment installment;
}
