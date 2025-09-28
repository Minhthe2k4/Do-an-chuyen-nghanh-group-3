package com.example.shop_management.DTO;

public class UserDTO {

    private Long id;
    private String username;
    private String password;
    private String roles;
    private Integer status;
    private Long credit_limit;

    public UserDTO(Long id, String username, String password, String roles, Integer status, Long credit_limit) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.status = status;
        this.credit_limit = credit_limit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public Long getCredit_limit() {
        return credit_limit;
    }

    public void setCredit_limit(Long credit_limit) {
        this.credit_limit = credit_limit;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
