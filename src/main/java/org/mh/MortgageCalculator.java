package org.mh;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public class MortgageCalculator {

    public static final int SCALE = 8;

    /**
     *
     * @param interestRates 利率变化历史
     * @param date 给定日期
     * @return 根据给定日期，返回该日期应该使用的利率
     */
    private static BigDecimal getInterestRateByDate(TreeMap<LocalDate, BigDecimal> interestRates, LocalDate date) {
        Entry<LocalDate, BigDecimal> floorEntry = interestRates.floorEntry(date);
        if (floorEntry == null) {
            throw new RuntimeException("Oops");
        }
        return floorEntry.getValue();
    }

    /**
     *
     * @param prepayments 提前还款历史
     * @param date 给定日期
     * @return 根据给定日期，返回该还款期内所有提前还款的累计值，均假设还款周期为一个月。
     */
    private static BigDecimal getPrepaymentByDate(TreeMap<LocalDate, BigDecimal> prepayments, LocalDate date) {
        BigDecimal accumulated = BigDecimal.ZERO;
        for (Entry<LocalDate, BigDecimal> kv : prepayments.entrySet()) {
            if (kv.getKey().compareTo(date.minusMonths(1)) < 0) {
                continue;
            }
            if (kv.getKey().compareTo(date) >= 0) {
                break;
            }
            accumulated = accumulated.add(kv.getValue());
        }
        return accumulated;
    }

    /**
     *
     * @param fixedPayments 固定还款还款额变化历史
     * @param date 给定日期
     * @return 根据给定日期，返回该日期所使用的固定还款额
     */
    private static BigDecimal getFixedPaymentByDate(TreeMap<LocalDate, BigDecimal> fixedPayments, LocalDate date) {
        if (fixedPayments == null) {
            return null;
        }
        Entry<LocalDate, BigDecimal> floorEntry = fixedPayments.floorEntry(date);
        if (floorEntry == null) {
            throw new RuntimeException("Oops");
        }
        return floorEntry.getValue();
    }

    /**
     * 计算等额本金模式下的月还款额详情
     * @param totalPrincipal 总本金
     * @param monthlyInterestRate 月利率
     * @param totalInstallments 总期数
     * @param currentInstallment 第几期（zero-based）
     * @param date 日期
     * @return 返回月还款额详情
     */
    private static MonthlyPayment calcEqualPrincipalPayment(BigDecimal totalPrincipal,
                                                            BigDecimal monthlyInterestRate,
                                                            int totalInstallments,
                                                            int currentInstallment,
                                                            LocalDate date) {
        // 月还款额中的本金部分：等额本金模式下，计算月还款额中的本金部分比较简单，直接使用 总本金/总期数 即可
        BigDecimal monthlyPrincipal = totalPrincipal.divide(new BigDecimal(totalInstallments), SCALE, RoundingMode.HALF_UP);
        // 月还款额中的利息部分：(总本金-已还本金)x月利率
        BigDecimal monthlyInterest = totalPrincipal
                .subtract(monthlyPrincipal.multiply(new BigDecimal(currentInstallment)))
                .multiply(monthlyInterestRate);
        return new MonthlyPayment(date, monthlyPrincipal, monthlyInterest);
    }

    /**
     * 计算等额本息模式下的月还款额详情
     * @param totalPrincipal 总本金
     * @param remainingPrincipal 剩余本金
     * @param monthlyInterestRate 月利率
     * @param totalInstallments 总期数
     * @param date 日期
     * @return 返回月还款额详情
     */
    private static MonthlyPayment calcEqualPrincipalAndInterestPayment(BigDecimal totalPrincipal,
                                                                       BigDecimal remainingPrincipal,
                                                                       BigDecimal monthlyInterestRate,
                                                                       int totalInstallments,
                                                                       LocalDate date) {
        BigDecimal pow = monthlyInterestRate.add(BigDecimal.ONE).pow(totalInstallments);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        // 月还款额：套用公式 总本金×月利率×(1+月利率)^总期数/ [(1+月利率)^总期数-1]
        BigDecimal monthlyPayment = totalPrincipal.multiply(monthlyInterestRate)
                .multiply(pow)
                .divide(denominator, SCALE, RoundingMode.HALF_UP);
        // 月还款额中的利息部分：剩余本金x月利率
        BigDecimal monthlyInterest = remainingPrincipal.multiply(monthlyInterestRate);
        return new MonthlyPayment(date, monthlyPayment.subtract(monthlyInterest), monthlyInterest);
    }

    /**
     * 计算固定还款额模式（）下的月还款额详情
     * @param monthlyPayment 月还款额
     * @param remainingPrincipal 剩余本金
     * @param monthlyInterestRate 月利率
     * @param date 日期
     * @return 返回月还款额详情
     */
    private static MonthlyPayment calcFixedPayment(BigDecimal monthlyPayment,
                                                   BigDecimal remainingPrincipal,
                                                   BigDecimal monthlyInterestRate,
                                                   LocalDate date) {
        // 月还款额中的利息部分：剩余本金x月利率
        BigDecimal monthlyInterest = remainingPrincipal.multiply(monthlyInterestRate);
        // 月还款额中的本金部分：月还款额-月利息
        BigDecimal monthlyPrincipal = monthlyPayment.subtract(monthlyInterest);
        return new MonthlyPayment(date, monthlyPrincipal, monthlyInterest);
    }

    /**
     * 计算不同还款模式下的月还款额
     * @param totalPrincipal 总本金
     * @param totalInstallments 总期数
     * @param interestRates 利率历史
     * @param prepayments 提前还款历史
     * @param fixedPayments 固定还款额
     * @param startDate 开始日期
     * @param paymentMethods 月还款类型
     * @return 自开始日期的各月份的还款额详情
     */
    public static List<MonthlyPayment> calcMonthlyPayments(BigDecimal totalPrincipal,
                                                            int totalInstallments,
                                                            TreeMap<LocalDate, BigDecimal> interestRates,
                                                            TreeMap<LocalDate, BigDecimal> prepayments,
                                                            TreeMap<LocalDate, BigDecimal> fixedPayments,
                                                            LocalDate startDate,
                                                            PaymentMethods paymentMethods) {
        List<MonthlyPayment> list = new ArrayList<>();

        BigDecimal principal = totalPrincipal;
        int installments = totalInstallments;
        BigDecimal lastRate = getInterestRateByDate(interestRates, startDate);
        BigDecimal remainingPrincipal = totalPrincipal;

        int installment = 0;
        LocalDate date;
        BigDecimal rate;
        BigDecimal prepayment;
        BigDecimal fixedPayment;
        for (int i = 0; i < totalInstallments; i++) {
            date = startDate.plusMonths(i);
            rate = getInterestRateByDate(interestRates, date);
            prepayment = getPrepaymentByDate(prepayments, date);
            fixedPayment = getFixedPaymentByDate(fixedPayments, date);

            // 当利率变化、或者提前还款发生时，需要使用剩余本金、新利率整体重新计算还款
            if (!rate.equals(lastRate) || !prepayment.equals(BigDecimal.ZERO)) {
                if (remainingPrincipal.compareTo(prepayment) <= 0) {
                    break;
                }
                remainingPrincipal = remainingPrincipal.subtract(prepayment);
                principal = remainingPrincipal;
                installments = totalInstallments - i;
                installment = 0;
                lastRate = rate;
            }

            MonthlyPayment payment;
            if (paymentMethods == PaymentMethods.EQUAL_PRINCIPAL_AND_INTEREST) {
                payment = calcEqualPrincipalAndInterestPayment(principal, remainingPrincipal, rate,
                        installments, date);
            } else if (paymentMethods == PaymentMethods.EQUAL_PRINCIPAL) {
                payment = calcEqualPrincipalPayment(principal, rate, installments, installment, date);
            } else if (paymentMethods == PaymentMethods.FIXED_PAYMENT) {
                if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                payment = calcFixedPayment(fixedPayment, remainingPrincipal, rate, date);
            } else {
                throw new RuntimeException("Not supported yet");
            }
            list.add(payment);
            installment++;
            remainingPrincipal = remainingPrincipal.subtract(payment.getPrincipal());
        }

        return list;
    }
}
