package com.example.shop_management.service;

import com.example.shop_management.model.Installment;
import com.example.shop_management.model.OrderHistory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOrderSuccessEmail(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 👇 Đây là chỗ set tên hiển thị cho email gửi đi
            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");

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


    public void sendOrderSuccessEmailViaCOD(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 👇 Đây là chỗ set tên hiển thị cho email gửi đi
            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");

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
                    .append("Qúy khách vui lòng theo dõi tình trạng đơn hàng qua website của chúng tôi, sản phẩm sẽ được giao trong vòng từ 3 - 5 ngày!<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            // 👇 gửi nội dung dạng HTML
            helper.setText(content.toString(), true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }


    public void sendOrderSuccessEmailViaSpayLater(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 👇 Đây là chỗ set tên hiển thị cho email gửi đi
            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn đã đặt hàng qua hình thức Spay later tại <b>Gian hàng thông minh</b> của chúng tôi.<br>")
                    .append("Thông tin đơn hàng của bạn như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Trạng thái: ").append(orderHistory.getStatus() == 1 ? "Đã thanh toán" : "Chưa thanh toán").append("<br><br>")
                    .append("Qúy khách vui lòng theo dõi tình trạng đơn hàng qua website của chúng tôi, sản phẩm sẽ được giao trong vòng từ 3 - 5 ngày!<br><br>")
                    .append("<b>Lưu ý: </b> Qúy khách cần xem thật kỹ các khoản thanh toán tại mục SpayLater tại mục Menu của trang chủ<br>")
                    .append("Nếu quý khách có bất cứ vấn đề nào về dịch vụ, vui lòng liên hệ qua email: gianhangthongminh@gmail.com<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            // 👇 gửi nội dung dạng HTML
            helper.setText(content.toString(), true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }


    public void sendCancelledOrderSuccessEmail(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 👇 Đây là chỗ set tên hiển thị cho email gửi đi
            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận hủy đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn sử dụng dịch vụ tại <b>Gian hàng thông minh</b> của chúng tôi.<br>")
                    .append("Đơn hàng của bạn đã hủy thành công!<br>")
                    .append("Thông tin đơn hàng mà bạn xác nhận hủy như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Nếu quý khách có bất cứ vấn đề nào về dịch vụ, vui lòng liên hệ qua email: gianhangthongminh@gmail.com<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            // 👇 gửi nội dung dạng HTML
            helper.setText(content.toString(), true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    public void sendInstallmentReminderEmail(String toEmail, Installment installment, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            var order = installment.getPayment().getOrderhistory();

            // 👇 Đây là chỗ set tên hiển thị cho email gửi đi
            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");

            helper.setTo(toEmail);
            helper.setSubject("Nhắc nhở thanh toán trả góp sắp đến hạn #" + orderHistory.getId());

            // 👉 Tính conversion fee = 2.95% của amount
            BigDecimal conversionFee = installment.getAmount().multiply(BigDecimal.valueOf(0.0295));

            // 👉 Tổng cộng
            BigDecimal total = installment.getAmount().add(conversionFee).add(installment.getLate_fee());

            StringBuilder content = new StringBuilder();
            content.append("<p>Xin chào ").append(order.getUser().getFull_name()).append(",</p>")
                    .append("<p>Khoản trả góp <b>#").append(installment.getInstallment_no()).append("</b> của bạn sắp đến hạn.</p>")
                    .append("<p>Ngày đến hạn: <b>")
                    .append(installment.getDue_date().toLocalDate()).append("</b></p>")
                    .append("<p>Số tiền cần thanh toán: <b>")
                    .append(total).append(" VND</b></p>")
                    .append("<p>Chi tiết:</p>")
                    .append("<ul>")
                    .append("<li>Gốc: ").append(installment.getAmount()).append(" VND</li>")
                    .append("<li>Phí quy đổi (2.95%): ").append(conversionFee).append(" VND</li>")
                    .append("<li>Phí trễ hạn: ").append(installment.getLate_fee()).append(" VND</li>")
                    .append("</ul>")
                    .append("<p>Vui lòng thanh toán đúng hạn để tránh phát sinh phí phạt.</p>")
                    .append("<br><p>Trân trọng,<br>Gian hàng thông minh</p>");

            // 👇 gửi nội dung dạng HTML
            helper.setText(content.toString(), true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }


}
