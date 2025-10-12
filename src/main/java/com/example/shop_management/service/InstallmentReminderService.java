package com.example.shop_management.service;

import com.example.shop_management.model.Installment;
import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.repository.InstallmentRepository;
import com.example.shop_management.repository.UserRepository;
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
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void remindUpcomingInstallments() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(3); // nhắc trước 3 ngày

        List<Installment> installments = installmentRepository.findInstallmentsDueOn(targetDate);

        System.out.println("Scheduled task running... Found " + installments.size() + " installments.");

        for (Installment i : installments) {
            try {
                OrderHistory orderHistory = i.getPayment().getOrderhistory();
                String toEmail = orderHistory.getUser().getEmail();
                Long userId = orderHistory.getUser().getId();
                Long installmentNo = i.getInstallment_no();

                System.out.println(">>> Sending mail to: " + toEmail + " for installment #" + installmentNo);

                // Gọi hàm gửi email với đầy đủ tham số
                emailService.sendInstallmentReminderEmail(toEmail, installmentNo, userId);

            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý installment id=" + i.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}