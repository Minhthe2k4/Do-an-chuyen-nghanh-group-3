package com.example.shop_management.controller.admin;

import com.example.shop_management.model.User;
import com.example.shop_management.repository.UserRepository;
import com.example.shop_management.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Map;

@Controller
public class AdminHomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/admin/home")
    public String home(Model model, Principal principal, @RequestParam(defaultValue = "2025") int year) {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        model.addAttribute("users", user);

        Map<String, Object> dashboardData = dashboardService.getDashboardData(year);
        model.addAttribute("totalRevenue", dashboardData.get("totalRevenue"));
        model.addAttribute("totalOrders", dashboardData.get("totalOrders"));
        model.addAttribute("totalCustomers", dashboardData.get("totalCustomers"));
        model.addAttribute("months", dashboardData.get("months"));
        model.addAttribute("revenues", dashboardData.get("revenues"));

        return "admin/index";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/auth/login";
    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model, Principal principal) {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("users", user);
        return "admin/index";
    }




}

