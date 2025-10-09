package com.example.shop_management.service;

import com.example.shop_management.model.Installment;
import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.repository.InstallmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InstallmentReminderService {

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private EmailService emailService;


    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void remindUpcomingInstallments() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(1); // nhắc trước 1 ngày để test

        List<Installment> installments = installmentRepository.findInstallmentsDueOn(targetDate);

        System.out.println("Scheduled task running... Found " + installments.size() + " installments.");

        for (Installment i : installments) {
            try {
                OrderHistory orderHistory = i.getPayment().getOrderhistory();
                String toEmail = orderHistory.getUser().getEmail();

                System.out.println(">>> Sending mail to: " + toEmail);

                // ✅ Gọi hàm gửi email
                emailService.sendInstallmentReminderEmail(toEmail, i,orderHistory);

            } catch (Exception e) {
                System.err.println("⚠️ Lỗi khi xử lý installment id=" + i.getId() + ": " + e.getMessage());
            }
        }
    }



}

