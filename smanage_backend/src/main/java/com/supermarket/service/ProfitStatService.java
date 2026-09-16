package com.supermarket.service;

import com.supermarket.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfitStatService {

    private final SaleOrderMapper saleOrderMapper;
    private final SaleRefundMapper saleRefundMapper;
    private final PurchaseRecordMapper purchaseRecordMapper;
    private final LossRecordMapper lossRecordMapper;
    private final ProductBatchDetailMapper batchDetailMapper;

    public Map<String, Object> getMonthlyProfit(Integer year, Integer month) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 销售收入
        BigDecimal monthlySalesRevenue = saleOrderMapper.selectMonthlyPaidTotal(year, month);
        monthlySalesRevenue = (monthlySalesRevenue != null) ? monthlySalesRevenue : BigDecimal.ZERO;
        result.put("monthlySalesRevenue", monthlySalesRevenue);

        // 退款金额
        BigDecimal monthlyRefundAmount = saleRefundMapper.selectMonthlyRefundTotal(year, month);
        monthlyRefundAmount = (monthlyRefundAmount != null) ? monthlyRefundAmount : BigDecimal.ZERO;
        result.put("monthlyRefundAmount", monthlyRefundAmount);

        // 实际收入 = 销售收入 - 退款
        BigDecimal monthlyActualRevenue = monthlySalesRevenue.subtract(monthlyRefundAmount);
        result.put("monthlyActualRevenue", monthlyActualRevenue);

        // 进货成本
        BigDecimal monthlyPurchaseAmount = purchaseRecordMapper.selectMonthlyPurchaseTotal(year, month);
        monthlyPurchaseAmount = (monthlyPurchaseAmount != null) ? monthlyPurchaseAmount : BigDecimal.ZERO;
        result.put("monthlyPurchaseAmount", monthlyPurchaseAmount);

        // 销售成本（已售出批次的进货成本总和，不含退货）
        BigDecimal monthlySoldCost = batchDetailMapper.selectMonthlySoldCostTotal(year, month);
        monthlySoldCost = (monthlySoldCost != null) ? monthlySoldCost : BigDecimal.ZERO;
        result.put("monthlySoldCost", monthlySoldCost);

        // 退货商品进货成本（已同意退货退回库存部分的成本）
        BigDecimal monthlyReturnCost = saleRefundMapper.selectMonthlyReturnCostTotal(year, month);
        monthlyReturnCost = (monthlyReturnCost != null) ? monthlyReturnCost : BigDecimal.ZERO;
        result.put("monthlyReturnCost", monthlyReturnCost);

        // 退货转损耗进货成本（已同意退货中转为损耗部分的成本）
        BigDecimal monthlyReturnLossCost = saleRefundMapper.selectMonthlyReturnLossCostTotal(year, month);
        monthlyReturnLossCost = (monthlyReturnLossCost != null) ? monthlyReturnLossCost : BigDecimal.ZERO;
        result.put("monthlyReturnLossCost", monthlyReturnLossCost);

        // 损耗成本（批次表中状态=3的进货成本，含退货转损耗）
        BigDecimal monthlyLossCost = batchDetailMapper.selectMonthlyLossCostTotal(year, month);
        monthlyLossCost = (monthlyLossCost != null) ? monthlyLossCost : BigDecimal.ZERO;
        result.put("monthlyLossCost", monthlyLossCost);

        // 损耗金额（loss_record表汇总）
        BigDecimal monthlyLossAmount = lossRecordMapper.selectMonthlyLossTotal(year, month);
        monthlyLossAmount = (monthlyLossAmount != null) ? monthlyLossAmount : BigDecimal.ZERO;
        result.put("monthlyLossAmount", monthlyLossAmount);

        // 损耗率
        BigDecimal lossRatio;
        if (monthlyPurchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            lossRatio = monthlyLossAmount
                    .divide(monthlyPurchaseAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            lossRatio = BigDecimal.ZERO;
        }
        result.put("lossRatio", lossRatio);

        // 销售净成本 = 销售成本（已自动排除退货，因为退货时批次status从2变更为1/3）
        // 损耗总成本 = 损耗成本（含退货转损耗部分）
        BigDecimal monthlyNetProfit = monthlyActualRevenue
                .subtract(monthlySoldCost)
                .subtract(monthlyLossCost);
        result.put("monthlyNetProfit", monthlyNetProfit);

        return result;
    }
}
