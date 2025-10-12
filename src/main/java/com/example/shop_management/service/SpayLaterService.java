package com.example.shop_management.service;

import com.example.shop_management.Enum.PaymentStatus;
import com.example.shop_management.model.Installment;
import com.example.shop_management.model.Payment;
import com.example.shop_management.repository.InstallmentRepository;
import com.example.shop_management.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpayLaterService {
    private final InstallmentRepository installmentRepo;
    private final PaymentRepository paymentRepo;

    private final double LATE_FEE = 30000; // 30k/kỳ trễ hạn
    private final double CONVERSION_FEE_RATE = 0.0295; // 2.95% phí chuyển đổi

    /**
     * Tạo các kỳ trả góp khi user chọn SPayLater
     *Tính 2.95% trên TỔNG, rồi lưu vào DB (mỗi kỳ lưu total = amount + phí_chung)
     */
    public void createInstallments(Payment payment, int installmentCount) {
        BigDecimal totalAmount = payment.getOrderhistory().getTotal_amount();

        // Tính phí chuyển đổi trên TỔNG (một lần duy nhất)
        BigDecimal totalConversionFee = totalAmount.multiply(BigDecimal.valueOf(CONVERSION_FEE_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        // Chia đều tiền gốc
        BigDecimal baseInstallment = totalAmount.divide(
                BigDecimal.valueOf(installmentCount),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal accumulated = baseInstallment.multiply(BigDecimal.valueOf(installmentCount));
        BigDecimal remainder = totalAmount.subtract(accumulated);

        LocalDateTime now = LocalDateTime.now();
        LocalDate baseDate = now.getDayOfMonth() >= 24
                ? now.toLocalDate().plusMonths(2)
                : now.toLocalDate().plusMonths(1);

        LocalDate safeDate = baseDate.withDayOfMonth(Math.min(10, baseDate.lengthOfMonth()));
        LocalDateTime dueDate = safeDate.atTime(23, 59);

        List<Installment> installments = new ArrayList<>();

        for (int i = 1; i <= installmentCount; i++) {
            Installment installment = new Installment();
            installment.setPayment(payment);
            installment.setInstallment_no((long) i);

            // BƯỚC 3: Tính amount cho kỳ này (tiền gốc)
            BigDecimal amount = baseInstallment;
            if (i == installmentCount && remainder.compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.add(remainder);
            }

            installment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            installment.setLate_fee(BigDecimal.ZERO);
            installment.setDue_date(dueDate);


            installment.setTotal(amount.add(totalConversionFee));


            installment.setPaid_fee(totalConversionFee);
            installments.add(installment);
            dueDate = dueDate.plusMonths(1);
        }

        installmentRepo.saveAll(installments);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepo.save(payment);
    }

    /**
     * Áp dụng phí trễ hạn: ghi nhận 30k vào kỳ tiếp theo
     * ĐÃ BỔ SUNG: Cập nhật lại total khi có late_fee
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void applyLateFeeForOverdueInstallments() {
        List<Installment> installments = installmentRepo.findAll();

        Map<Long, List<Installment>> groupedByPayment = installments.stream()
                .collect(Collectors.groupingBy(i -> i.getPayment().getId()));

        for (Map.Entry<Long, List<Installment>> entry : groupedByPayment.entrySet()) {
            List<Installment> paymentInstallments = entry.getValue().stream()
                    .sorted(Comparator.comparing(Installment::getDue_date))
                    .toList();

            for (int i = 0; i < paymentInstallments.size(); i++) {
                Installment current = paymentInstallments.get(i);

                // kiểm tra đủ điều kiện: chưa trả và đến hạn hoặc quá hạn
                if (!current.isPaid() && !current.getDue_date().isAfter(LocalDateTime.now())) {
                    BigDecimal fee = BigDecimal.valueOf(LATE_FEE);

                    if (i + 1 < paymentInstallments.size()) {
                        Installment next = paymentInstallments.get(i + 1);
                        BigDecimal newLateFee = next.getLate_fee() == null ? fee : next.getLate_fee().add(fee);
                        next.setLate_fee(newLateFee);

                        //CẬP NHẬT LẠI TOTAL KHI CÓ LATE_FEE
                        BigDecimal conversionFee = next.getTotal().subtract(next.getAmount());
                        next.setTotal(next.getAmount().add(conversionFee).add(newLateFee));

                        installmentRepo.save(next);
                    } else {
                        BigDecimal newLateFee = current.getLate_fee() == null ? fee : current.getLate_fee().add(fee);
                        current.setLate_fee(newLateFee);

                        // CẬP NHẬT LẠI TOTAL KHI CÓ LATE_FEE
                        BigDecimal conversionFee = current.getTotal().subtract(current.getAmount());
                        current.setTotal(current.getAmount().add(conversionFee).add(newLateFee));

                        installmentRepo.save(current);
                    }
                }
            }
        }
    }
}