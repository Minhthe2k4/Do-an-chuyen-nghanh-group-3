package com.example.shop_management.service;

import com.example.shop_management.model.OrderHistory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOrderSuccessEmail(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 👇 Đây là chỗ set tên hiển thị cho email gửi đi
            helper.setFrom("shopABC@gmail.com", "Gian hàng thông minh");

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn đã đặt hàng tại <b>Gian hàng thông minh</b>.<br>")
                    .append("Thông tin đơn hàng của bạn như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Trạng thái: ").append(orderHistory.getStatus() == 1 ? "Đã thanh toán" : "Chưa thanh toán").append("<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            // 👇 gửi nội dung dạng HTML
            helper.setText(content.toString(), true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }
}
