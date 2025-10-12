package com.example.shop_management.repository;

import com.example.shop_management.model.Product;
import com.example.shop_management.DTO.ProductDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


    @Query("SELECT new com.example.shop_management.DTO.ProductDTO(" +
            "p.id, p.item_name, p.item_image, c.category_name, p.item_price, p.stock_quantity) " +
            "FROM Product p JOIN p.category c")
    List<ProductDTO> getProductInfo();


    // Lấy tất cả sản phẩm theo category id
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Integer categoryId);

    //Tìm kiếm theo tên sản phẩm (không phân biệt hoa thường)
    @Query("SELECT p FROM Product p WHERE LOWER(p.item_name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByName(@Param("keyword") String keyword);

    // Tìm sản phẩm theo tên item_name (dùng native query cho chắc)
    @Query(value = "SELECT * FROM items WHERE item_name = :itemName LIMIT 1", nativeQuery = true)
    Product findByItemName(@Param("itemName") String itemName);


}
