package com.example.shop_management.controller.user;

import com.example.shop_management.DTO.ProductDTO;
import com.example.shop_management.model.Category;
import com.example.shop_management.model.Product;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.CategoryRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserHomeController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping("/home")
    public String home(@RequestParam(value = "category", required = false) Integer categoryId, @RequestParam(value = "keyword", required = false) String keyword, Model model, Principal principal) {
        String username = principal.getName();
        User users = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));


        //Loc san pham theo category
        List<Category> categories = categoryRepository.findAll();
        List<Product> products;

        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);

            //Tim san pham theo ten san pham
        } else if (keyword != null) {
            products = productRepository.searchByName(keyword);
        } else {
            products = productRepository.findAll();
        }


        //Tim kiem san pham theo ten san pham


        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("users", users);

        return "user/index";
    }

        @GetMapping("/list-product")
    public String showProductInfo(@RequestParam(value = "category", required = false) Integer categoryId, Model model, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

            List<Category> categories = categoryRepository.findAll();
            List<Product> products;

            if (categoryId != null) {
                products = productRepository.findByCategoryId(categoryId);
            } else {
                products = productRepository.findAll();
            }

            model.addAttribute("categories", categories);
            model.addAttribute("products", products);
            model.addAttribute("selectedCategory", categoryId);


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

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // 🔹 Service chỉ xử lý logic thêm vào giỏ
            cartService.addItemToCart(user.getId(), itemId, quantity);

            redirectAttributes.addFlashAttribute("success", "Added to cart successfully!");

        } catch (RuntimeException e) {
            // 🔹 Nếu service throw lỗi (ví dụ hết hàng)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/list-product";
    }


}
