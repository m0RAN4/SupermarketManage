package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.entity.ProductBase;
import com.supermarket.entity.ProductBatchDetail;
import com.supermarket.entity.SaleOrder;
import com.supermarket.entity.SaleRefund;
import com.supermarket.mapper.ProductBaseMapper;
import com.supermarket.mapper.ProductBatchDetailMapper;
import com.supermarket.mapper.SaleOrderMapper;
import com.supermarket.mapper.SaleRefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductBatchDetailMapper batchDetailMapper;
    private final ProductBaseMapper productBaseMapper;
    private final SaleOrderMapper saleOrderMapper;
    private final SaleRefundMapper saleRefundMapper;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        LambdaQueryWrapper<ProductBatchDetail> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.in(ProductBatchDetail::getStatus, Arrays.asList(0, 1));
        Long totalProducts = batchDetailMapper.selectCount(stockWrapper);
        result.put("totalProducts", totalProducts != null ? totalProducts : 0);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<SaleOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.between(SaleOrder::getCreateTime, todayStart, todayEnd);
        Long todayOrders = saleOrderMapper.selectCount(orderWrapper);
        result.put("todayOrders", todayOrders != null ? todayOrders : 0);

        LambdaQueryWrapper<ProductBase> baseWrapper = new LambdaQueryWrapper<>();
        baseWrapper.eq(ProductBase::getIsEnabled, 1);
        List<ProductBase> enabledProducts = productBaseMapper.selectList(baseWrapper);
        long warningCount = 0;
        for (ProductBase pb : enabledProducts) {
            LambdaQueryWrapper<ProductBatchDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(ProductBatchDetail::getBaseProductId, pb.getId())
                         .in(ProductBatchDetail::getStatus, Arrays.asList(0, 1));
            Long count = batchDetailMapper.selectCount(detailWrapper);
            if (count != null && count < 10) {
                warningCount++;
            }
        }
        result.put("stockWarning", warningCount);

        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        LambdaQueryWrapper<SaleOrder> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.in(SaleOrder::getPayStatus, Arrays.asList(1, 4))
                   .ge(SaleOrder::getCreateTime, LocalDateTime.of(year, month, 1, 0, 0))
                   .lt(SaleOrder::getCreateTime, LocalDateTime.of(month + 1 > 12 ? year + 1 : year, month + 1 > 12 ? 1 : month + 1, 1, 0, 0));
        List<SaleOrder> paidOrders = saleOrderMapper.selectList(paidWrapper);
        BigDecimal paidRevenue = paidOrders.stream()
                .map(SaleOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<SaleRefund> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(SaleRefund::getRefundHandleStatus, 1)
                     .ge(SaleRefund::getCreateTime, LocalDateTime.of(year, month, 1, 0, 0))
                     .lt(SaleRefund::getCreateTime, LocalDateTime.of(month + 1 > 12 ? year + 1 : year, month + 1 > 12 ? 1 : month + 1, 1, 0, 0));
        List<SaleRefund> refunds = saleRefundMapper.selectList(refundWrapper);
        BigDecimal refundAmount = refunds.stream()
                .map(SaleRefund::getRefundTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("monthlyProfit", paidRevenue.subtract(refundAmount));

        return result;
    }
}
