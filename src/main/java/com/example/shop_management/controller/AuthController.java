package com.example.shop_management.controller;

import com.example.shop_management.model.User;
import com.example.shop_management.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // GET /auth/login
    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login"; // resources/templates/auth/login.html
    }

    // GET /auth/register
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "auth/register"; // resources/templates/auth/register.html
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/auth/login";
    }



    // POST /auth/register
    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user,
                           @RequestParam("confirmPassword") String confirmPassword,
                           RedirectAttributes ra) {

        // check confirm password
        if (!user.getPassword().equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu và xác nhận mật khẩu không khớp!");
            ra.addFlashAttribute("user", user);
            return "redirect:/auth/register";
        }

        boolean success = userService.register(user);

        if (success) {
            ra.addFlashAttribute("success", "Đăng ký thành công! Mời bạn đăng nhập.");
            return "redirect:/auth/login";
        } else {
            ra.addFlashAttribute("error", "Username đã tồn tại. Vui lòng chọn tên khác!");
            ra.addFlashAttribute("user", user);
            return "redirect:/auth/register";
        }
    }
}
