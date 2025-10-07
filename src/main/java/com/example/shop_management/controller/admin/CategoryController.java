package com.example.shop_management.controller.admin;

import com.example.shop_management.DTO.CategoryDTO;
import com.example.shop_management.model.Category;
import com.example.shop_management.model.Product;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.CategoryRepository;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;


    private final CategoryService categoryService;

    @Autowired
    private UserRepository userRepository;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list-category")
    public String ListCategory(Model model, Principal principal) {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        List<CategoryDTO> categories = categoryRepository.getCategoryInfo();
        model.addAttribute("categories", categories);
        return "admin/ecommerce-categories";
    }

    // Form thêm sản phẩm
    @GetMapping("/add-category")
    public String showAddForm(Model model, Principal principal) {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        model.addAttribute("categoriessss", new Category());
        return "admin/forms-add-categories";
    }

    //Xử lý thêm sản phẩm
    @PostMapping("/add-category")
    public String addCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        // Kiểm tra trùng tên
        if (categoryService.categoryExists(category.getCategory_name())) {
            redirectAttributes.addFlashAttribute("error", "Category name already exists!");
            return "redirect:/list-category";
        }

        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryService.addCategory(category);

        redirectAttributes.addFlashAttribute("success", "Category added successfully!");
        return "redirect:/list-category";
    }


    @GetMapping("/edit-category/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Principal principal) {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        model.addAttribute("category", categoryService.getCategoryById(id));
        return "admin/forms-edit-categories";
    }

    // Xử lý cập nhật danh mục
    @PostMapping("/edit-category/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @ModelAttribute Category category,
                                 RedirectAttributes redirectAttributes) {
        categoryService.updateCategory(id, category);

        // Thông báo thành công
        redirectAttributes.addFlashAttribute("success", "Category updated successfully!");
        return "redirect:/list-category";
    }

    // Xóa danh mục
    @GetMapping("/delete-category/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.deleteCategory(id);

        // Thông báo thành công
        redirectAttributes.addFlashAttribute("success", "Category deleted successfully!");
        return "redirect:/list-category";
    }
}
