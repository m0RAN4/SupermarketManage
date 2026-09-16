package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.ProductBase;
import com.supermarket.entity.ProductBatchDetail;
import com.supermarket.entity.PurchaseRecord;
import com.supermarket.mapper.ProductBaseMapper;
import com.supermarket.mapper.ProductBatchDetailMapper;
import com.supermarket.mapper.PurchaseRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRecordMapper purchaseRecordMapper;
    private final ProductBaseMapper productBaseMapper;
    private final ProductBatchDetailMapper batchDetailMapper;

    public Map<String, Object> page(Integer current, Integer size, Long baseProductId, String startDate, String endDate) {
        Page<PurchaseRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        if (baseProductId != null) {
            wrapper.eq(PurchaseRecord::getBaseProductId, baseProductId);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge(PurchaseRecord::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le(PurchaseRecord::getCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(PurchaseRecord::getCreateTime);
        Page<PurchaseRecord> rawPage = purchaseRecordMapper.selectPage(page, wrapper);

        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (PurchaseRecord pr : rawPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", pr.getId());
            item.put("baseProductId", pr.getBaseProductId());
            item.put("purchaseQuantity", pr.getPurchaseQuantity());
            item.put("purchaseUnitPrice", pr.getPurchaseUnitPrice());
            item.put("purchaseTotalAmount", pr.getPurchaseTotalAmount());
            item.put("createTime", pr.getCreateTime());

            if (pr.getBaseProductId() != null) {
                ProductBase pb = productBaseMapper.selectById(pr.getBaseProductId());
                item.put("baseProductName", pb != null ? pb.getProductName() : "-");
            } else {
                item.put("baseProductName", "-");
            }
            enrichedRecords.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", rawPage.getTotal());
        result.put("current", rawPage.getCurrent());
        result.put("size", rawPage.getSize());
        result.put("pages", rawPage.getPages());
        return result;
    }

    @Transactional
    public void add(Map<String, Object> purchaseData) {
        Long baseProductId = purchaseData.get("baseProductId") != null
                ? Long.valueOf(purchaseData.get("baseProductId").toString()) : null;
        Integer purchaseQuantity = purchaseData.get("purchaseQuantity") != null
                ? Integer.valueOf(purchaseData.get("purchaseQuantity").toString()) : null;
        BigDecimal purchaseUnitPrice = purchaseData.get("purchaseUnitPrice") != null
                ? new BigDecimal(purchaseData.get("purchaseUnitPrice").toString()) : null;

        if (baseProductId == null) {
            throw new BusinessException("请选择商品");
        }
        ProductBase productBase = productBaseMapper.selectById(baseProductId);
        if (productBase == null) {
            throw new BusinessException("商品不存在");
        }
        if (productBase.getIsEnabled() != 1) {
            throw new BusinessException("该商品已被禁用，无法进货");
        }
        if (purchaseQuantity == null || purchaseQuantity <= 0) {
            throw new BusinessException("进货数量必须大于0");
        }
        if (purchaseUnitPrice == null || purchaseUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("进货单价不能为空且不能为负");
        }

        // 创建进货记录
        PurchaseRecord record = new PurchaseRecord();
        record.setBaseProductId(baseProductId);
        record.setPurchaseQuantity(purchaseQuantity);
        record.setPurchaseUnitPrice(purchaseUnitPrice);
        record.setPurchaseTotalAmount(purchaseUnitPrice.multiply(BigDecimal.valueOf(purchaseQuantity)));
        purchaseRecordMapper.insert(record);

        // 按进货数量生成对应批次的明细记录（不填展示名称，上架时才填）
        for (int i = 0; i < purchaseQuantity; i++) {
            ProductBatchDetail detail = new ProductBatchDetail();
            detail.setPurchaseRecordId(record.getId());
            detail.setBaseProductId(baseProductId);
            detail.setShelfId(null);
            detail.setShelfLayer(null);
            detail.setShelfDisplayName(null); // 进货不写展示名称
            detail.setPurchaseUnitPrice(purchaseUnitPrice);
            detail.setSalePrice(BigDecimal.ZERO);
            detail.setDiscountPrice(null);
            detail.setStatus(0);
            detail.setBelongOrderItemId(null);
            batchDetailMapper.insert(detail);
        }
    }
}
