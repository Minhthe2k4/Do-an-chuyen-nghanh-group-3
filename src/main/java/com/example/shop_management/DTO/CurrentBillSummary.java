package com.example.shop_management.DTO;

import com.example.shop_management.model.Installment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CurrentBillSummary {
    private BigDecimal total_amount;
    private BigDecimal total_fee;
    private BigDecimal total_late_fee;
    private List<Installment> installments;
}

