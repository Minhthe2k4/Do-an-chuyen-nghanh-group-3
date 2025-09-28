package com.example.shop_management.controller.user;

import com.example.shop_management.DTO.ProductDTO;
import com.example.shop_management.model.Product;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.ProductRepository;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserHomeController {

    @Autowired
    private ProductRepository productRepository;

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        String username = principal.getName();
        List<ProductDTO> products = productRepository.getProductInfo();

        User users = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", users);
        model.addAttribute("product", products);
        return "user/index";
    }

    @GetMapping("/list-product")
    public String showProductInfo(Model model, Principal principal) {
        List<ProductDTO> products = productRepository.getProductInfo();
        model.addAttribute("product", products);
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "user/index";
    }

    @GetMapping("/detailed-product/{id}")
    public String showDetailedProductInfo(@PathVariable("id") Long id, Model model, Principal principal) {
        Optional<Product> products = productRepository.findById(id);
        if (products.isPresent()) {
            Product productss = products.get();
            model.addAttribute("product", productss);
            String username = principal.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            model.addAttribute("users", user);
            return "user/product-detail";
        } else {
            return "redirect:/list-product?error=notfound";
        }

    }


    @PostMapping("/add-to-cart")
    public String addToCart(
            @RequestParam("itemId") Long itemId,
            @RequestParam("quantity") int quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        // Lấy user từ login
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Thêm vào giỏ
        cartService.addItemToCart(user.getId(), itemId, quantity);

        // Thêm flash attribute de tao thong bao cho html
        redirectAttributes.addFlashAttribute("successMessage", "Added to cart successfully");

        // Redirect về trang danh sách
        return "redirect:/user/list-product";
    }


}
