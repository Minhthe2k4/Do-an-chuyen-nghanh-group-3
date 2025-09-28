package com.example.shop_management.controller.user;

import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.OrderHistoryRepository;
import com.example.shop_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/user")
public class OrderUserController {

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/myorder")
    public String viewOrder(Model model, Principal principal) {
        String username = principal.getName();

        // Lấy user hiện tại
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Chỉ lấy order của user đó
        List<OrderHistory> orderHistories = orderHistoryRepository.findByUserId(user.getId());

        model.addAttribute("orderhistories", orderHistories);
        model.addAttribute("users", user);
        return "user/myorder";
    }


    @Transactional
    @GetMapping("/myorder/delete/{id}")
    public String deleteOrder(@PathVariable("id") Long orderId,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        // Lấy user hiện tại
        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy orderHistory
        OrderHistory orderHistory = orderHistoryRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra order có thuộc về user hay không
        if (!orderHistory.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You don't have the right to delete this order!");
            return "redirect:/user/myorder";
        }


        // Xóa order history (MySQL sẽ cascade sang payments, installments, transactions)
        orderHistoryRepository.delete(orderHistory);

        // Đảm bảo flush (Hibernate xóa ngay, không chờ)
        orderHistoryRepository.flush();

        redirectAttributes.addFlashAttribute("success", "The order was deleted and refunded successfully");
        return "redirect:/user/myorder";
    }




}
