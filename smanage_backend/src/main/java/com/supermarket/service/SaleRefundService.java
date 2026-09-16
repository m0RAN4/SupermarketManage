package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.dto.RefundDTO;
import com.supermarket.entity.*;
import com.supermarket.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SaleRefundService {

    private final SaleRefundMapper saleRefundMapper;
    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final ProductBatchDetailMapper batchDetailMapper;
    private final LossRecordMapper lossRecordMapper;
    private final ProductBaseMapper productBaseMapper;
    private final UserInfoMapper userInfoMapper;
    private final SaleOrderService saleOrderService;
    private final ShelfCapacityTrackingMapper capacityTrackingMapper;

    public Map<String, Object> page(Integer pageNum, Integer pageSize, Long orderId, Integer handleStatus, String startTime, String endTime) {
        Page<SaleRefund> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SaleRefund> wrapper = new LambdaQueryWrapper<>();
        if (orderId != null) {
            wrapper.eq(SaleRefund::getOrderId, orderId);
        }
        if (handleStatus != null) {
            wrapper.eq(SaleRefund::getRefundHandleStatus, handleStatus);
        }
        if (startTime != null && !startTime.trim().isEmpty()) {
            wrapper.ge(SaleRefund::getCreateTime, startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            wrapper.le(SaleRefund::getCreateTime, endTime + " 23:59:59");
        }
        wrapper.orderByDesc(SaleRefund::getCreateTime);
        Page<SaleRefund> rawPage = saleRefundMapper.selectPage(page, wrapper);

        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (SaleRefund sr : rawPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", sr.getId());
            item.put("orderId", sr.getOrderId());
            item.put("orderItemId", sr.getOrderItemId());
            item.put("batchDetailId", sr.getBatchDetailId());
            item.put("operatorId", sr.getOperatorId());
            item.put("refundQuantity", sr.getRefundQuantity());
            item.put("refundUnitPrice", sr.getRefundUnitPrice());
            item.put("refundTotalAmount", sr.getRefundTotalAmount());
            item.put("refundReason", sr.getRefundReason());
            item.put("refundHandleStatus", sr.getRefundHandleStatus());
            item.put("backStockQuantity", sr.getBackStockQuantity());
            item.put("toLossQuantity", sr.getToLossQuantity());
            item.put("createTime", sr.getCreateTime());

            UserInfo operator = userInfoMapper.selectById(sr.getOperatorId());
            item.put("operatorName", operator != null ? operator.getUsername() : "-");

            // 批次信息
            if (sr.getBatchDetailId() != null) {
                ProductBatchDetail batch = batchDetailMapper.selectById(sr.getBatchDetailId());
                if (batch != null) {
                    item.put("shelfDisplayName", batch.getShelfDisplayName());
                    if (batch.getBaseProductId() != null) {
                        ProductBase base = productBaseMapper.selectById(batch.getBaseProductId());
                        item.put("productName", base != null ? base.getProductName() : "-");
                    }
                }
            }
            if (!item.containsKey("productName")) {
                item.put("productName", "-");
            }

            SaleOrderItem oi = saleOrderItemMapper.selectById(sr.getOrderItemId());
            if (oi != null) {
                item.put("buyQuantity", oi.getBuyQuantity());
                item.put("buyUnitPrice", oi.getBuyUnitPrice());
            }

            enrichedRecords.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", rawPage.getTotal());
        result.put("current", rawPage.getCurrent());
        result.put("size", rawPage.getSize());
        result.put("pages", rawPage.getPages());
        return result;
    }

    @Transactional
    public void refund(RefundDTO dto) {
        if (dto.getOrderId() == null) {
            throw new BusinessException("订单ID不能为空");
        }
        SaleOrder order = saleOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getPayStatus() != 1 && order.getPayStatus() != 4) {
            throw new BusinessException("订单状态不允许退款");
        }
        if (dto.getOrderItemId() == null) {
            throw new BusinessException("订单商品详情ID不能为空");
        }
        SaleOrderItem item = saleOrderItemMapper.selectById(dto.getOrderItemId());
        if (item == null) {
            throw new BusinessException("订单商品详情不存在");
        }
        if (dto.getRefundQuantity() == null || dto.getRefundQuantity() <= 0) {
            throw new BusinessException("退款数量必须大于0");
        }
        // 检查是否存在未处理的退款申请，防止重复提交
        List<SaleRefund> existingRefunds = saleRefundMapper.selectByOrderItemId(dto.getOrderItemId());
        long pendingCount = existingRefunds.stream()
                .filter(r -> r.getRefundHandleStatus() == 0)
                .mapToInt(r -> r.getRefundQuantity() != null ? r.getRefundQuantity() : 0)
                .sum();
        if (pendingCount > 0) {
            throw new BusinessException("该商品已有 " + pendingCount + " 件待审核的退款申请，请等待管理员处理后再提交");
        }

        int currentRefunded = item.getRefundQuantity() != null ? item.getRefundQuantity() : 0;
        if (currentRefunded + dto.getRefundQuantity() > item.getBuyQuantity()) {
            throw new BusinessException("退款数量超过可退数量");
        }

        BigDecimal refundUnitPrice = item.getBuyUnitPrice();
        BigDecimal refundTotalAmount = refundUnitPrice.multiply(BigDecimal.valueOf(dto.getRefundQuantity()));

        SaleRefund refund = new SaleRefund();
        refund.setOrderId(dto.getOrderId());
        refund.setOrderItemId(dto.getOrderItemId());
        refund.setBatchDetailId(item.getBatchDetailId());
        refund.setOperatorId(dto.getOperatorId());
        refund.setRefundQuantity(dto.getRefundQuantity());
        refund.setRefundUnitPrice(refundUnitPrice);
        refund.setRefundTotalAmount(refundTotalAmount);
        refund.setRefundReason(dto.getRefundReason());
        refund.setRefundHandleStatus(0);
        refund.setBackStockQuantity(0);
        refund.setToLossQuantity(0);
        saleRefundMapper.insert(refund);
    }

    @Transactional
    public void approveRefund(Long refundId, Integer backStockQuantity, Integer toLossQuantity) {
        if (backStockQuantity == null) backStockQuantity = 0;
        if (toLossQuantity == null) toLossQuantity = 0;

        SaleRefund refund = saleRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退货记录不存在");
        }
        if (refund.getRefundHandleStatus() != 0) {
            throw new BusinessException("该退货申请已处理");
        }

        int total = backStockQuantity + toLossQuantity;
        if (total != refund.getRefundQuantity()) {
            throw new BusinessException("回库数量(" + backStockQuantity + ") + 转损耗数量(" + toLossQuantity
                    + ") = " + total + "，必须等于退货数量(" + refund.getRefundQuantity() + ")");
        }

        SaleOrderItem orderItem = saleOrderItemMapper.selectById(refund.getOrderItemId());
        if (orderItem == null) {
            throw new BusinessException("订单详情不存在");
        }

        // 获取该订单项关联的已售出批次
        List<ProductBatchDetail> soldBatches = batchDetailMapper.selectByOrderItemId(orderItem.getId());
        int processed = 0;

        // 回库部分：恢复容量追踪
        for (int i = 0; i < backStockQuantity && processed < soldBatches.size(); i++, processed++) {
            ProductBatchDetail batch = soldBatches.get(processed);
            batchDetailMapper.markAsReturnedToStock(batch.getId());
            // 恢复容量追踪（回库时恢复到原货架原层）
            if (batch.getShelfId() != null && batch.getShelfLayer() != null) {
                capacityTrackingMapper.incrementUsed(batch.getShelfId(), batch.getShelfLayer(), 1);
            }
        }

        // 转损耗部分：不恢复容量
        for (int i = 0; i < toLossQuantity && processed < soldBatches.size(); i++, processed++) {
            ProductBatchDetail batch = soldBatches.get(processed);
            batchDetailMapper.markAsLoss(batch.getId());

            LossRecord loss = new LossRecord();
            loss.setBatchDetailId(batch.getId());
            loss.setLossReason("退货转损耗（退货ID:" + refundId + "）");
            loss.setLossQuantity(1);
            loss.setLossUnitPrice(batch.getPurchaseUnitPrice());
            loss.setLossTotalAmount(batch.getPurchaseUnitPrice());
            lossRecordMapper.insert(loss);
        }

        refund.setRefundHandleStatus(1);
        refund.setBackStockQuantity(backStockQuantity);
        refund.setToLossQuantity(toLossQuantity);
        saleRefundMapper.updateById(refund);

        orderItem.setRefundQuantity((orderItem.getRefundQuantity() != null ? orderItem.getRefundQuantity() : 0) + refund.getRefundQuantity());
        saleOrderItemMapper.updateById(orderItem);

        // 更新订单退款状态
        saleOrderService.updateRefundStatus(refund.getOrderId());
    }

    @Transactional
    public void rejectRefund(Long refundId) {
        SaleRefund refund = saleRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退货记录不存在");
        }
        if (refund.getRefundHandleStatus() != 0) {
            throw new BusinessException("该退货申请已处理");
        }
        refund.setRefundHandleStatus(2);
        saleRefundMapper.updateById(refund);
    }

    /** 批量审批退货 */
    @Transactional
    public void batchApprove(List<Long> refundIds, List<Integer> backStockQuantities, List<Integer> toLossQuantities) {
        if (refundIds == null || refundIds.isEmpty()) {
            throw new BusinessException("请选择要审批的退货记录");
        }
        for (int i = 0; i < refundIds.size(); i++) {
            approveRefund(refundIds.get(i),
                    backStockQuantities != null && i < backStockQuantities.size() ? backStockQuantities.get(i) : 0,
                    toLossQuantities != null && i < toLossQuantities.size() ? toLossQuantities.get(i) : 0);
        }
    }

    /** 批量拒绝退货 */
    @Transactional
    public void batchReject(List<Long> refundIds) {
        if (refundIds == null || refundIds.isEmpty()) {
            throw new BusinessException("请选择要拒绝的退货记录");
        }
        for (Long id : refundIds) {
            rejectRefund(id);
        }
    }
}
