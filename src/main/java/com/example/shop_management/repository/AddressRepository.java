package com.example.shop_management.repository;

import com.example.shop_management.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findTopByUserIdOrderByCreatedAtDesc(Long userId);

}
