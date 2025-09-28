package com.example.shop_management.repository;


import com.example.shop_management.model.Cart;
import com.example.shop_management.model.CartItem;
import com.example.shop_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);


    List<CartItem> findByCartId(Long cartId);

}
