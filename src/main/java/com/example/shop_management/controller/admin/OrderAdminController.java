package com.example.shop_management.controller.admin;

import com.example.shop_management.DTO.OrderHistoryDTO;
import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.OrderHistoryRepository;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.OrderHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/ecommerce-order")
public class OrderAdminController {

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private OrderHistoryService orderHistoryService;

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


        // Hiển thị form sửa trạng thái đơn hàng

        @GetMapping("/edit-order/{id}")
        public String showEditOrderForm(@PathVariable Long id, Model model, Principal principal) {
            String username = principal.getName();
            OrderHistory order = orderHistoryService.getOrderHistoryById(id);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            model.addAttribute("users", user);
            model.addAttribute("order", order);
            return "admin/forms-edit-orders";
        }

        // Xử lý cập nhật sản phẩm
        @PostMapping("/edit-order/{id}")
        public String updateOrder(@PathVariable Long id, @ModelAttribute OrderHistory orderHistory, RedirectAttributes redirectAttributes) {

            orderHistoryService.updateOrderHistory(id, orderHistory);

            // thêm thông báo
            redirectAttributes.addFlashAttribute("success", "Order updated successfully!");
            return "redirect:/admin/ecommerce-order";
        }


    @GetMapping("/logout")
    public String logout() {
        return "redirect:/auth/login";
    }


}
