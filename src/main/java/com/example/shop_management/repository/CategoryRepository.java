package com.example.shop_management.repository;

import com.example.shop_management.DTO.CategoryDTO;
import com.example.shop_management.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT new com.example.shop_management.DTO.CategoryDTO(c.id, c.category_name) FROM Category c")
    List<CategoryDTO> getCategoryInfo();

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.category_name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

}
