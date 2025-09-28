package com.example.shop_management.DTO;


public class CategoryDTO {

    private Long id;
    private String category_name;

    public CategoryDTO(Long id, String category_name) {
        this.id = id;
        this.category_name = category_name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }
}
