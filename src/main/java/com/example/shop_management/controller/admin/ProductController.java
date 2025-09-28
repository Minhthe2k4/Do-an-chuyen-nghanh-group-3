package com.example.shop_management.controller.admin;

import com.example.shop_management.DTO.ProductDTO;
import com.example.shop_management.model.Product;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.CategoryRepository;
import com.example.shop_management.repository.ProductRepository;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;


    @Autowired
    private CategoryRepository categoryRepository;


    private final ProductService productService;

    @Autowired
    private UserRepository userRepository;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/list-product")
    public String ListProduct(Model model, Principal principal) {
        List<ProductDTO> products = productRepository.getProductInfo();
        model.addAttribute("products", products);
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/ecommerce-products";
    }


    @GetMapping("/detailed-product/{id}")
    public String showDetailedProductInfo(@PathVariable("id") Long id, Model model, Principal principal) {
        Optional<Product> products = productRepository.findById(id);
        if(products.isPresent()) {
            Product productss = products.get();
            model.addAttribute("product", productss);
            String username = principal.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            model.addAttribute("users", user);
            return "admin/ecommerce-product-details";
        } else {
            return "redirect:/ecommerce-products?error=notfound";
        }

    }

    // Form thêm sản phẩm
    @GetMapping("/add-product")
    public String showAddForm(Model model, Principal principal) {
        model.addAttribute("productss", new Product());
        model.addAttribute("categoriess", categoryRepository.findAll());
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/forms-add-products";
    }

    // Xử lý thêm sản phẩm
    @PostMapping("/add-product")
    public String addProduct(@ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productService.addProduct(product);

        // thêm thông báo
        redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        return "redirect:/list-product";

    }

    @GetMapping("/edit-product/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("product", productService.getProductById(id));
        model.addAttribute("category", categoryRepository.findAll());

        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/forms-edit-products";
    }

    // Xử lý cập nhật sản phẩm
    @PostMapping("/edit-product/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        productService.updateProduct(id, product);

        // thêm thông báo
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        return "redirect:/list-product";
    }

    // Xóa sản phẩm
    @GetMapping("/delete-product/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);

        // thêm thông báo
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
        return "redirect:/list-product";
    }
}
