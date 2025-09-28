package com.example.shop_management.service;

import com.example.shop_management.model.Product;
import com.example.shop_management.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Service
public class ProductService {

    private ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with Id" + id));
    }

    public Product updateProduct(Long id, Product updateProduct) {
        Product existingProduct = getProductById(id);
        existingProduct.setItem_code(updateProduct.getItem_code());
        existingProduct.setItem_name(updateProduct.getItem_name());
        existingProduct.setCategory(updateProduct.getCategory());
        existingProduct.setItem_price(updateProduct.getItem_price());
        existingProduct.setItem_image(updateProduct.getItem_image());
        existingProduct.setItem_description(updateProduct.getItem_description());
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }


}
