package com.example.shop_management.service;


import com.example.shop_management.model.User;
import com.example.shop_management.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return false; // Username đã tồn tại
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles("USER"); // mặc định
        user.setStatus(1);
        user.setCredit_limit(BigDecimal.valueOf(10000000L));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        return true;
    }

    public User addUser(User user) {
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with Id" + id));
    }

    public User updateUser(Long id, User updateUser) {
        User existingUser = getUserById(id);
        existingUser.setUsername(updateUser.getUsername());
        existingUser.setPassword(updateUser.getPassword());
        existingUser.setRoles(updateUser.getRoles());
        existingUser.setStatus(updateUser.getStatus());
        existingUser.setCredit_limit(updateUser.getCredit_limit());
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}

