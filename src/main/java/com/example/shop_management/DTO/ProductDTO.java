package com.example.shop_management.DTO;

public class ProductDTO {

    private Long id;
    private String item_name;
    private String item_image;
    private String category_name;
    private Long item_price;

    public ProductDTO(Long id, String item_name, String item_image, String category_name, Long item_price) {
        this.id = id;
        this.item_name = item_name;
        this.item_image = item_image;
        this.category_name = category_name;
        this.item_price = item_price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }

    public String getItem_image() {
        return item_image;
    }

    public void setItem_image(String item_image) {
        this.item_image = item_image;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }

    public Long getItem_price() {
        return item_price;
    }

    public void setItem_price(Long item_price) {
        this.item_price = item_price;
    }
}
