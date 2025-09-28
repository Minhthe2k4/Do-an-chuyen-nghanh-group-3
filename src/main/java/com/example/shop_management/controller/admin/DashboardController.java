package com.example.shop_management.controller.admin;

import com.example.shop_management.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    //Hiện dashboard
    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "2025") int year, Model model) {
        Map<String, Object> dashboardData = dashboardService.getDashboardData(year);

        model.addAttribute("totalRevenue", dashboardData.get("totalRevenue"));
        model.addAttribute("totalOrders", dashboardData.get("totalOrders"));
        model.addAttribute("totalCustomers", dashboardData.get("totalCustomers"));
        model.addAttribute("months", dashboardData.get("months"));
        model.addAttribute("revenues", dashboardData.get("revenues"));

        return "admin/index";
    }
}

