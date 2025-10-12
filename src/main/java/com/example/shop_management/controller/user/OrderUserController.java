package com.example.shop_management.controller.user;

import com.example.shop_management.Enum.PaymentMethod;
import com.example.shop_management.model.*;
import com.example.shop_management.repository.*;
import com.example.shop_management.service.EmailService;
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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private EmailService emailService;



    @GetMapping("/myorder")
    public String viewOrder(Model model,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        // Lấy username từ principal (Spring Security)
        String username = principal.getUsername();

        // Luôn lấy user mới nhất từ DB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Lấy danh sách order của user
        List<OrderHistory> orderHistories = orderHistoryRepository.findByUserId(user.getId());

        // Truyền dữ liệu ra view
        model.addAttribute("users", user);              // user mới nhất (đã update credit_limit)
        model.addAttribute("orderhistories", orderHistories);

        return "user/myorder";
    }

    @Transactional
    @GetMapping("/myorder/delete/{id}")
    public String deleteOrder(@PathVariable("id") Long orderId,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            String username = principal.getUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Tìm đơn hàng
            OrderHistory orderHistory = orderHistoryRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Lấy thông tin thanh toán
            Payment payment = paymentRepository.findByOrderhistoryId(orderId);

            // Cộng lại stock dựa vào order_items
            String orderItems = orderHistory.getOrder_items(); // VD: "Áo thun x2, Quần jean x1"
            if (orderItems != null && !orderItems.isBlank()) {
                String[] items = orderItems.split(",\\s*");
                for (String itemStr : items) {
                    String[] parts = itemStr.split(" x");
                    if (parts.length == 2) {
                        String productName = parts[0].trim();
                        int quantity = Integer.parseInt(parts[1].trim());
                        Product product = productRepository.findByItemName(productName);
                        if (product != null) {
                            product.setStock_quantity(product.getStock_quantity() + quantity);
                            productRepository.save(product);
                        }
                    }
                }
                productRepository.flush();
            }

            // Xóa order của user
            orderHistoryRepository.deleteByIdAndUserId(orderId, user.getId());

            // Nếu thanh toán SPayLater thì cộng lại credit limit
            if (payment != null && payment.getPayment_method() == PaymentMethod.SPAY_LATER) {
                BigDecimal orderAmount = orderHistory.getTotal_amount();
                if (orderAmount != null) {
                    user.setCredit_limit(user.getCredit_limit().add(orderAmount));
                    userRepository.save(user);
                }
            }

            emailService.sendCancelledOrderSuccessEmail(user.getEmail(), orderHistory);

            redirectAttributes.addFlashAttribute("success", "The order was cancelled successfully");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        return "redirect:/user/myorder";
    }

}
