package org.mh;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class MonthlyPayment {
    private static final DecimalFormat df = new DecimalFormat("#.00");

    private LocalDate date;
    private BigDecimal principal; // 每月所还本金
    private BigDecimal interest; // 每月所还利息

    public BigDecimal getPayment() {
        return principal.add(interest);
    }

    @Override
    public String toString() {
        return date + " " + df.format(getPayment()) + " (本 = " + df.format(principal) + ", 息 = " + df.format(interest) + ")";
    }
}
