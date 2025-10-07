package com.example.shop_management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String addressLine;


    private String province_name;

    private String district_name;

    private String ward_name;

    // Liên kết với user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "address")
    private List<OrderHistory> orderHistories = new ArrayList<>();

    @Column(length = 20)
    private String postalCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
