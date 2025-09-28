package com.example.shop_management.service;

import com.example.shop_management.model.Category;
import com.example.shop_management.model.Product;
import com.example.shop_management.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CategoryService {

    private CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with Id" + id));
    }

    public Category updateCategory(Long id, Category updateCategory) {
        Category existingProduct = getCategoryById(id);
        existingProduct.setCategory_name(updateCategory.getCategory_name());
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.save(existingProduct);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
