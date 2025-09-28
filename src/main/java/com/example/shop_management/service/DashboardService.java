package com.example.shop_management.service;

import com.example.shop_management.repository.OrderHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> getDashboardData(int year) {
        Map<String, Object> data = new HashMap<>();

        // Doanh thu theo tháng của năm cụ thể
        String jpql = "SELECT MONTH(o.created_at), SUM(o.total_amount) " +
                "FROM OrderHistory o " +
                "WHERE YEAR(o.created_at) = :year " +
                "GROUP BY MONTH(o.created_at) " +
                "ORDER BY MONTH(o.created_at)";

        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("year", year)
                .getResultList();

        List<Integer> months = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();

        for (Object[] row : results) {
            months.add((Integer) row[0]);
            revenues.add(((BigDecimal) row[1]).doubleValue());
        }

        data.put("months", months);
        data.put("revenues", revenues);

        // Tổng số liệu cho cả năm
        Long totalOrders = entityManager.createQuery(
                        "SELECT COUNT(o) FROM OrderHistory o WHERE YEAR(o.created_at) = :year", Long.class)
                .setParameter("year", year)
                .getSingleResult();

        Long totalCustomers = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT o.user) FROM OrderHistory o WHERE YEAR(o.created_at) = :year", Long.class)
                .setParameter("year", year)
                .getSingleResult();

        BigDecimal totalRevenue = entityManager.createQuery(
                        "SELECT SUM(o.total_amount) FROM OrderHistory o WHERE YEAR(o.created_at) = :year", BigDecimal.class)
                .setParameter("year", year)
                .getSingleResult();


        data.put("totalOrders", totalOrders);
        data.put("totalCustomers", totalCustomers);
        data.put("totalRevenue", totalRevenue);

        return data;
    }
}
