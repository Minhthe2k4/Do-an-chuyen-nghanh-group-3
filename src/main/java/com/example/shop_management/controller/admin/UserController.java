package com.example.shop_management.controller.admin;

import com.example.shop_management.model.Category;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;



    @GetMapping("/list-user")
    public String ListUser(Model model, Principal principal) {
        List<User> usersss = userRepository.findAll();
        model.addAttribute("userss", usersss);
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/ecommerce-users";
    }

    // Form thêm sản phẩm
    @GetMapping("/add-user")
    public String showAddForm(Model model, Principal principal) {
        model.addAttribute("user", new User());
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/forms-add-users";
    }

    // Xử lý thêm user
    @PostMapping("/add-user")
    public String addUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        user.setCredit_limit(BigDecimal.valueOf(10000000L));
        user.setRoles("USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.addUser(user);

        // Thông báo
        redirectAttributes.addFlashAttribute("successMessage", "User added successfully!");
        return "redirect:/list-user";
    }

    @GetMapping("/edit-user/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("user", userService.getUserById(id));
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/forms-edit-users";
    }

    // Xử lý cập nhật user
    @PostMapping("/edit-user/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             RedirectAttributes redirectAttributes) {
        user.setCredit_limit(BigDecimal.valueOf(10000000L));
        user.setRoles("USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now()); // cái này nếu muốn giữ createdAt gốc thì không set lại
        user.setUpdatedAt(LocalDateTime.now());
        userService.updateUser(id, user);

        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        return "redirect:/list-user";
    }

    // Xóa user
    @GetMapping("/delete-user/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);

        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/list-user";
    }
}
