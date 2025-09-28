package com.example.shop_management.repository;

import com.example.shop_management.model.Cart;
import com.example.shop_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);


    Optional<Cart> findByUserId(Long userId);




}
