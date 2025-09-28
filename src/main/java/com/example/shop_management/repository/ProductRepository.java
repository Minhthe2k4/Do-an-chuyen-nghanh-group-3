package com.example.shop_management.repository;

import com.example.shop_management.model.Product;
import com.example.shop_management.DTO.ProductDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;



@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


    // @Query("SELECT new com.example.shop_management.model.ProductDTO(p.id, p.item_name, p.item_image, c.category_name, p.item_price) FROM Product p JOIN p.category c")
    @Query("SELECT new com.example.shop_management.DTO.ProductDTO(" +
            "p.id, p.item_name, p.item_image, c.category_name, p.item_price) " +
            "FROM Product p JOIN p.category c")
    List<ProductDTO> getProductInfo();





}
