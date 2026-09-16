package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.LossRecord;
import com.supermarket.entity.ProductBase;
import com.supermarket.entity.ProductBatchDetail;
import com.supermarket.mapper.LossRecordMapper;
import com.supermarket.mapper.ProductBaseMapper;
import com.supermarket.mapper.ProductBatchDetailMapper;
import com.supermarket.mapper.ShelfCapacityTrackingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LossRecordService {

    private final LossRecordMapper lossRecordMapper;
    private final ProductBatchDetailMapper batchDetailMapper;
    private final ProductBaseMapper productBaseMapper;
    private final ShelfCapacityTrackingMapper capacityTrackingMapper;

    public Map<String, Object> page(Integer pageNum, Integer pageSize, Long baseProductId, String startTime, String endTime) {
        Page<LossRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LossRecord> wrapper = new LambdaQueryWrapper<>();

        if (startTime != null && !startTime.trim().isEmpty()) {
            wrapper.ge(LossRecord::getCreateTime, startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            wrapper.le(LossRecord::getCreateTime, endTime + " 23:59:59");
        }

        // baseProductId 过滤下推到 SQL 层，确保分页 total 准确
        if (baseProductId != null) {
            List<ProductBatchDetail> batches = batchDetailMapper.selectByBaseProductId(baseProductId);
            List<Long> batchDetailIds = batches.stream().map(ProductBatchDetail::getId).toList();
            if (batchDetailIds.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("records", List.of());
                empty.put("total", 0L);
                empty.put("current", (long) pageNum);
                empty.put("size", (long) pageSize);
                return empty;
            }
            wrapper.in(LossRecord::getBatchDetailId, batchDetailIds);
        }

        wrapper.orderByDesc(LossRecord::getCreateTime);
        Page<LossRecord> rawPage = lossRecordMapper.selectPage(page, wrapper);

        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (LossRecord lr : rawPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", lr.getId());
            item.put("batchDetailId", lr.getBatchDetailId());
            item.put("lossReason", lr.getLossReason());
            item.put("lossQuantity", lr.getLossQuantity());
            item.put("lossUnitPrice", lr.getLossUnitPrice());
            item.put("lossTotalAmount", lr.getLossTotalAmount());
            item.put("createTime", lr.getCreateTime());

            if (lr.getBatchDetailId() != null) {
                ProductBatchDetail batch = batchDetailMapper.selectById(lr.getBatchDetailId());
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

            enrichedRecords.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", rawPage.getTotal());
        result.put("current", rawPage.getCurrent());
        result.put("size", rawPage.getSize());
        return result;
    }

    @Transactional
    public void add(Map<String, Object> lossData) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) lossData.get("batchDetailIds");
        if (rawIds == null || rawIds.isEmpty()) {
            throw new BusinessException("请选择要损耗的商品批次");
        }
        List<Long> batchDetailIds = rawIds.stream().map(Number::longValue).toList();
        String lossReason = lossData.get("lossReason") != null
                ? lossData.get("lossReason").toString() : null;

        if (lossReason == null || lossReason.trim().isEmpty()) {
            throw new BusinessException("请输入损耗原因");
        }

        for (Long batchId : batchDetailIds) {
            ProductBatchDetail batch = batchDetailMapper.selectById(batchId);
            if (batch == null) {
                throw new BusinessException("批次不存在: " + batchId);
            }
            if (batch.getStatus() != 0 && batch.getStatus() != 1) {
                throw new BusinessException("只能损耗在库或已上架的商品，批次 " + batchId + " 状态异常");
            }
            batchDetailMapper.markAsLoss(batch.getId());

            // 如果是已上架商品（status=1），同步更新货架容量追踪
            if (batch.getStatus() == 1 && batch.getShelfId() != null && batch.getShelfLayer() != null) {
                capacityTrackingMapper.decrementUsed(batch.getShelfId(), batch.getShelfLayer(), 1);
            }

            LossRecord loss = new LossRecord();
            loss.setBatchDetailId(batch.getId());
            loss.setLossReason(lossReason.trim());
            loss.setLossQuantity(1);
            loss.setLossUnitPrice(batch.getPurchaseUnitPrice());
            loss.setLossTotalAmount(batch.getPurchaseUnitPrice());
            lossRecordMapper.insert(loss);
        }
    }

    public Map<String, Object> getMonthlyStats(Integer year, Integer month) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal lossTotalAmount = lossRecordMapper.selectMonthlyLossTotal(year, month);
        result.put("lossTotalAmount", lossTotalAmount != null ? lossTotalAmount : BigDecimal.ZERO);

        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        LambdaQueryWrapper<LossRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(LossRecord::getCreateTime, startOfMonth)
               .le(LossRecord::getCreateTime, endOfMonth);
        Long lossCount = lossRecordMapper.selectCount(wrapper);
        result.put("lossCount", lossCount != null ? lossCount : 0);

        return result;
    }
}
