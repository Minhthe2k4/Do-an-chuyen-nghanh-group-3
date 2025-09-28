package com.example.shop_management.service;

import com.example.shop_management.model.Cart;
import com.example.shop_management.model.CartItem;
import com.example.shop_management.model.Product;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.CartItemRepository;
import com.example.shop_management.repository.CartRepository;
import com.example.shop_management.repository.ProductRepository;
import com.example.shop_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;


    @Transactional
    public Cart addItemToCart(Long userId, Long productId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Tìm hoặc tạo Cart
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUser(user); // ⚡ BẮT BUỘC
                    c.setCreated_at(LocalDateTime.now());
                    c.setUpdated_at(LocalDateTime.now());
                    return cartRepository.save(c);
                });

        // Tìm hoặc tạo CartItem
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setCart(cart);
                    ci.setProduct(product);
                    ci.setQuantity(0);
                    ci.setCreated_at(LocalDateTime.now());
                    ci.setUpdated_at(LocalDateTime.now());
                    return ci;
                });

        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItem.setUpdated_at(LocalDateTime.now());
        cartItemRepository.save(cartItem);

        return cart;
    }




    public Cart getCartByUser(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user); // ⚡ bắt buộc set
                    return cartRepository.save(newCart);
                });
    }


    public void updateItemQuantity(Long userId, Long itemId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("CartItem not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    public void removeItemFromCart(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("CartItem not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        cartItemRepository.delete(item);
    }



}

