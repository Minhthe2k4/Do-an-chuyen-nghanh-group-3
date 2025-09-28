package com.example.shop_management.controller.user;

import com.example.shop_management.model.Cart;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.CartService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/user")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/cart")
    public String viewCart(
            Model model,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            Principal principall
    ) {
        String username = principal.getUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("users", user);

        Cart cart = cartService.getCartByUser(user);

        int total = 0;
        if (cart.getCartItem() != null) {
            total = cart.getCartItem().stream()
                    .mapToInt(item -> Math.toIntExact(item.getProduct().getItem_price() * item.getQuantity()))
                    .sum();
        }

        model.addAttribute("cart", cart);
        model.addAttribute("total", total);

        return "user/cart";
    }

    @GetMapping("/cart/checkout")
    public String viewCheckOut(
            Model model,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal
    ) {
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



    @PostMapping("/update")
    public String updateCartItem(
            @RequestParam("itemId") Long itemId,
            @RequestParam("quantity") int quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        cartService.updateItemQuantity(user.getId(), itemId, quantity);

        redirectAttributes.addFlashAttribute("successMessage", "Update the number of product successfully");
        return "redirect:/user/cart";
    }

    @GetMapping("/remove/{itemId}")
    public String removeFromCart(
            @PathVariable("itemId") Long itemId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        cartService.removeItemFromCart(user.getId(), itemId);

        redirectAttributes.addFlashAttribute("successMessage", "Remove the item from cart successfully");
        return "redirect:/user/cart";
    }


}
