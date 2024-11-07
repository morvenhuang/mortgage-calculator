package org.mh;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public class Examples {

    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("#.00");

        System.out.println("[[等额本金]]");
        List<MonthlyPayment> equalPrincipalRst = equalPrincipalExample();
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        for (MonthlyPayment monthlyPayment : equalPrincipalRst) {
            totalPrincipal = totalPrincipal.add(monthlyPayment.getPrincipal());
            totalInterest = totalInterest.add(monthlyPayment.getInterest());
            System.out.println(monthlyPayment + "  已还总本金(不含提前还款): " + df.format(totalPrincipal) + "  已还总利息: " + df.format(totalInterest));
        }
        System.out.println();

        System.out.println("[[等额本息]]");
        List<MonthlyPayment> equalPrincipalAndInterestRst = equalPrincipalAndInterestExample();
        totalInterest = BigDecimal.ZERO;
        totalPrincipal = BigDecimal.ZERO;
        for (MonthlyPayment monthlyPayment : equalPrincipalAndInterestRst) {
            totalPrincipal = totalPrincipal.add(monthlyPayment.getPrincipal());
            totalInterest = totalInterest.add(monthlyPayment.getInterest());
            System.out.println(monthlyPayment + "  已还总本金(不含提前还款): " + df.format(totalPrincipal) + "  已还总利息: " + df.format(totalInterest));
        }
        System.out.println();

        System.out.println("[[固定还款]]");
        List<MonthlyPayment> fixedPaymentRst = fixedPaymentExample();
        totalInterest = BigDecimal.ZERO;
        totalPrincipal = BigDecimal.ZERO;
        for (MonthlyPayment monthlyPayment : fixedPaymentRst) {
            totalPrincipal = totalPrincipal.add(monthlyPayment.getPrincipal());
            totalInterest = totalInterest.add(monthlyPayment.getInterest());
            System.out.println(monthlyPayment + "  已还总本金(不含提前还款): " + df.format(totalPrincipal) + "  已还总利息: " + df.format(totalInterest));
        }
    }

    /**
     * 等额本金还款明细全列表示例。通常包含以下输入信息：
     * 1. 贷款金额
     * 2. 贷款年限/贷款期数
     * 3. 首次还款时间
     * 4. 利率变化历史，需要提供从首次还款开始后的所有利率变化历史
     * 5. 提前还款历史（可选），目前大部分银行允许进行多次提前还款。
     *
     * 注意：在利率变动/提前还款之后的次月（仅次月），实际还款的金额可能与计算略有不同，这主要是利息补偿（由于利率变动/提前还款日期与正常约定还款日期不一致导致的）
     */
    private static List<MonthlyPayment> equalPrincipalExample() {
        BigDecimal principal = new BigDecimal(890000); // 贷款金额89万
        int installments = 300; // 贷款年限25年，即300期

        // 利率变化（以下所举例子为交通银行（北京市）房贷利率变化真实信息，其他地区或银行或有不同情况）
        TreeMap<LocalDate, BigDecimal> interestRates = new TreeMap<>();
        // 月利率=年利率/12
        // 目前，房贷通常使用LPR利率，可在LPR基础上上浮/下降n个基点。同时，LPR变更通常在次年1月1日生效。
        // 以下以2022年北京市部分银行二套房利率：(LPR + 105基点)为例，105基点即万分之105，即1.05%或0.0105。
        // 2022年初，LPR=0.046，0.046+0.0105=0.0565
        interestRates.put(LocalDate.of(2022, 1, 1), new BigDecimal("0.0565").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2022年末，LPR=0.043，0.043+0.0105=0.0535
        interestRates.put(LocalDate.of(2023, 1, 1), new BigDecimal("0.0535").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2023年10月份，北京市政策变更，部分二套可认定为一套，假设你所购房符合一套认定，则利率变为：(LPR + 55基点)，即 0.043+0.0055=0.0485
        interestRates.put(LocalDate.of(2023, 11, 1), new BigDecimal("0.0485").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2023年末，LPR=0.042，0.042+0.0055=0.0475
        interestRates.put(LocalDate.of(2024, 1, 1), new BigDecimal("0.0475").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2024年10月末，政策变动，存量房贷（一套）利率调整为(LPR - 30基点)，0.042-0.003=0.039
        interestRates.put(LocalDate.of(2024, 11, 1), new BigDecimal("0.039").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));

        // 假设在以下时间节点进行了提前还款
        TreeMap<LocalDate, BigDecimal> prepayments = new TreeMap<>();
        prepayments.put(LocalDate.of(2024, 1, 6), new BigDecimal("180000"));
        prepayments.put(LocalDate.of(2024, 4, 2), new BigDecimal("200000"));
        prepayments.put(LocalDate.of(2024, 9, 3), new BigDecimal("200000"));

        // 假设首次还款时间为 2022年4月24日
        LocalDate startDate = LocalDate.of(2022, 4, 24);

        List<MonthlyPayment> monthlyPayments = MortgageCalculator.calcMonthlyPayments(principal,
                installments,
                interestRates,
                prepayments,
                null,
                startDate,
                PaymentMethods.EQUAL_PRINCIPAL);
        return monthlyPayments;
    }

    /**
     * 等额本息还款明细全列表示例。通常包含以下输入信息：
     * 1. 贷款金额
     * 2. 贷款年限/贷款期数
     * 3. 首次还款时间
     * 4. 利率变化历史，需要提供从首次还款开始后的所有利率变化历史
     * 5. 提前还款历史（可选），目前大部分银行允许进行多次提前还款。
     *
     * 注意：在利率变动/提前还款之后的次月（仅次月），实际还款的金额可能与计算略有不同，这主要是利息补偿（由于利率变动/提前还款日期与正常约定还款日期不一致导致的）
     */
    private static List<MonthlyPayment> equalPrincipalAndInterestExample() {
        BigDecimal principal = new BigDecimal(890000);
        int installments = 300;

        // 利率变化（以下所举例子为交通银行（北京市）房贷利率变化真实信息，其他地区或银行或有不同情况）
        TreeMap<LocalDate, BigDecimal> interestRates = new TreeMap<>();
        // 月利率=年利率/12
        // 目前，房贷通常使用LPR利率，可在LPR基础上上浮/下降n个基点。同时，LPR变更通常在次年1月1日生效。
        // 以下以2022年北京市部分银行二套房利率：(LPR + 105基点)为例，105基点即万分之105，即1.05%或0.0105。
        // 2022年初，LPR=0.046，0.046+0.0105=0.0565
        interestRates.put(LocalDate.of(2022, 1, 1), new BigDecimal("0.0565").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2022年末，LPR=0.043，0.043+0.0105=0.0535
        interestRates.put(LocalDate.of(2023, 1, 1), new BigDecimal("0.0535").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2023年10月份，北京市政策变更，部分二套可认定为一套，假设你所购房符合一套认定，则利率变为：(LPR + 55基点)，即 0.043+0.0055=0.0485
        interestRates.put(LocalDate.of(2023, 11, 1), new BigDecimal("0.0485").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2023年末，LPR=0.042，0.042+0.0055=0.0475
        interestRates.put(LocalDate.of(2024, 1, 1), new BigDecimal("0.0475").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        // 2024年10月末，政策变动，存量房贷（一套）利率调整为(LPR - 30基点)，0.042-0.003=0.039
        interestRates.put(LocalDate.of(2024, 11, 1), new BigDecimal("0.039").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));

        // 假设在以下时间节点进行了提前还款
        TreeMap<LocalDate, BigDecimal> prepayments = new TreeMap<>();
        prepayments.put(LocalDate.of(2024, 1, 6), new BigDecimal("180000"));
        prepayments.put(LocalDate.of(2024, 4, 2), new BigDecimal("200000"));
        prepayments.put(LocalDate.of(2024, 9, 3), new BigDecimal("200000"));

        LocalDate startDate = LocalDate.of(2022, 4, 24);

        List<MonthlyPayment> monthlyPayments = MortgageCalculator.calcMonthlyPayments(principal,
                installments,
                interestRates,
                prepayments,
                null,
                startDate,
                PaymentMethods.EQUAL_PRINCIPAL_AND_INTEREST);
        return monthlyPayments;
    }

    /**
     * 固定金额还款明细全列表示例，目前公积金（HPF, Housing Provident Fund）贷款采用该还款模式。通常包含以下输入信息：
     * 1. 贷款金额
     * 2. 贷款年限/贷款期数
     * 3. 首次还款时间
     * 4. 利率变化历史，需要提供从首次还款开始后的所有利率变化历史
     * 5. 月供金额变化历史，本还款模式下，可以是随时变更月还款额
     * 6. 提前还款历史（可选），目前大部分银行允许进行多次提前还款。
     *
     * 注意：在利率变动/提前还款之后的次月（仅次月），实际还款的金额可能与计算略有不同，这主要是利息补偿（由于利率变动/提前还款日期与正常约定还款日期不一致导致的）
     */
    private static List<MonthlyPayment> fixedPaymentExample() {
        // （公积金）贷款金额60万
        BigDecimal principal = new BigDecimal(600000);
        // 贷款年限25年，即300期
        int installments = 300;

        // 利率变化（公积金）
        TreeMap<LocalDate, BigDecimal> interestRatesHPF = new TreeMap<>();
        interestRatesHPF.put(LocalDate.of(2022, 1, 1), new BigDecimal("0.03575").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));
        interestRatesHPF.put(LocalDate.of(2025, 1, 1), new BigDecimal("0.03075").divide(new BigDecimal(12), MortgageCalculator.SCALE, RoundingMode.HALF_UP));

        // 月供金额变化
        TreeMap<LocalDate, BigDecimal> fixedPayments = new TreeMap<>();
        fixedPayments.put(LocalDate.of(2022, 1, 1), new BigDecimal("2461"));
        fixedPayments.put(LocalDate.of(2025, 8, 1), new BigDecimal("2000"));
        fixedPayments.put(LocalDate.of(2026, 8, 1), new BigDecimal("500"));

        // 假设在以下时间节点进行了提前还款
        TreeMap<LocalDate, BigDecimal> prepaymentsHPF = new TreeMap<>();
        prepaymentsHPF.put(LocalDate.of(2025, 8, 1), new BigDecimal("200000"));
        prepaymentsHPF.put(LocalDate.of(2026, 8, 1), new BigDecimal("300000"));

        LocalDate startDate = LocalDate.of(2022, 4, 24);

        List<MonthlyPayment> monthlyPayments = MortgageCalculator.calcMonthlyPayments(principal,
                installments,
                interestRatesHPF,
                prepaymentsHPF,
                fixedPayments,
                startDate,
                PaymentMethods.FIXED_PAYMENT);
        return monthlyPayments;
    }
}
