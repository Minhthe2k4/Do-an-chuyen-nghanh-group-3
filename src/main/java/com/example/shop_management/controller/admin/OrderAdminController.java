package com.example.shop_management.controller.admin;

import com.example.shop_management.DTO.OrderHistoryDTO;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.OrderHistoryRepository;
import com.example.shop_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/ecommerce-order")
public class OrderAdminController {

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    //Hiện thông tin đơn hàng cho Admin
    @GetMapping
    public String showOrderInfo(Model model, Principal principal) {
        List<OrderHistoryDTO> orders = orderHistoryRepository.getProduct();
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        model.addAttribute("orders", orders);
        return "admin/ecommerce-orders";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/auth/login";
    }


}
