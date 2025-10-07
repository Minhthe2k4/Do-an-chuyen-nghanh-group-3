package com.example.shop_management.controller.user;

import com.example.shop_management.model.Cart;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    // 🛒 Hiển thị giỏ hàng
    @GetMapping("/cart")
    public String viewCart(Model model,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartService.getCartByUser(user);

        int total = 0;
        if (cart.getCartItem() != null) {
            total = cart.getCartItem().stream()
                    .mapToInt(item -> Math.toIntExact(item.getProduct().getItem_price() * item.getQuantity()))
                    .sum();
        }

        model.addAttribute("users", user);
        model.addAttribute("cart", cart);
        model.addAttribute("total", total);
        return "user/cart";
    }

    // 💳 Hiển thị trang thanh toán
    @GetMapping("/cart/checkout")
    public String viewCheckOut(Model model,
                               @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartService.getCartByUser(user);

        int subtotal = 0;
        if (cart != null && cart.getCartItem() != null) {
            subtotal = cart.getCartItem().stream()
                    .mapToInt(item -> Math.toIntExact(item.getProduct().getItem_price() * item.getQuantity()))
                    .sum();
        }

        model.addAttribute("users", user);
        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("total", subtotal);
        return "user/checkout";
    }

    // ➕ Thêm sản phẩm vào giỏ
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("itemId") Long itemId,
                            @RequestParam("quantity") int quantity,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            cartService.addItemToCart(user.getId(), itemId, quantity);
            redirectAttributes.addFlashAttribute("success", "Added to cart successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/list-product";
    }

    // ✏️ Cập nhật số lượng sản phẩm trong giỏ
    @PostMapping("/update")
    public String updateCartItem(@RequestParam("itemId") Long itemId,
                                 @RequestParam("quantity") int quantity,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            cartService.updateItemQuantity(user.getId(), itemId, quantity);
            redirectAttributes.addFlashAttribute("success", "Updated product quantity successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/cart";
    }

    // ❌ Xóa sản phẩm khỏi giỏ hàng
    @GetMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable("itemId") Long itemId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            cartService.removeItemFromCart(user.getId(), itemId);
            redirectAttributes.addFlashAttribute("success", "Removed item from cart successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/cart";
    }
}
