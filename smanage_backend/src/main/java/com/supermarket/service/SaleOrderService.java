package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.dto.OrderItemDTO;
import com.supermarket.entity.*;
import com.supermarket.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SaleOrderService {

    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final ProductBatchDetailMapper batchDetailMapper;
    private final ProductBaseMapper productBaseMapper;
    private final ShelfInfoMapper shelfInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final ShelfCapacityTrackingMapper capacityTrackingMapper;
    private final SaleRefundMapper saleRefundMapper;

    public Map<String, Object> page(Integer pageNum, Integer pageSize, Integer payStatus, String startTime, String endTime, Long userId) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SaleOrder::getUserId, userId);
        }
        if (payStatus != null) {
            wrapper.eq(SaleOrder::getPayStatus, payStatus);
        }
        if (startTime != null && !startTime.trim().isEmpty()) {
            wrapper.ge(SaleOrder::getCreateTime, startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            wrapper.le(SaleOrder::getCreateTime, endTime + " 23:59:59");
        }
        wrapper.orderByDesc(SaleOrder::getCreateTime);
        Page<SaleOrder> rawPage = saleOrderMapper.selectPage(page, wrapper);

        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (SaleOrder order : rawPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", order.getId());
            item.put("userId", order.getUserId());
            item.put("payAmount", order.getPayAmount());
            item.put("payStatus", order.getPayStatus());
            item.put("createTime", order.getCreateTime());

            UserInfo user = userInfoMapper.selectById(order.getUserId());
            item.put("username", user != null ? user.getUsername() : "-");

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

    public SaleOrder getById(Long id) {
        return saleOrderMapper.selectById(id);
    }

    @Transactional
    public Long createOrder(Long userId, List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("订单商品不能为空");
        }

        BigDecimal orderTotal = BigDecimal.ZERO;
        List<SaleOrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO dto : items) {
            if (dto.getBaseProductId() == null) {
                throw new BusinessException("商品ID不能为空");
            }
            if (dto.getBuyQuantity() == null || dto.getBuyQuantity() <= 0) {
                throw new BusinessException("购买数量必须大于0");
            }

            // 用前端传来的 batchId 精确获取该批次的价格（用户点了哪个商品就用哪个的标价）
            BigDecimal unitPrice = null;
            if (dto.getBatchId() != null) {
                ProductBatchDetail selectedBatch = batchDetailMapper.selectById(dto.getBatchId());
                if (selectedBatch != null) {
                    unitPrice = selectedBatch.getDiscountPrice() != null && selectedBatch.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
                            ? selectedBatch.getDiscountPrice()
                            : selectedBatch.getSalePrice() != null && selectedBatch.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                            ? selectedBatch.getSalePrice() : null;
                }
            }
            // 购物车商品都有确定的 batchId，价格直接从批次获取即可
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException("商品价格信息异常");
            }

            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(dto.getBuyQuantity()));
            orderTotal = orderTotal.add(itemTotal);

            SaleOrderItem orderItem = new SaleOrderItem();
            orderItem.setBaseProductId(dto.getBaseProductId());
            orderItem.setBuyQuantity(dto.getBuyQuantity());
            orderItem.setBuyUnitPrice(unitPrice);
            orderItem.setItemTotalAmount(itemTotal);
            orderItem.setRefundQuantity(0);
            orderItems.add(orderItem);
        }

        SaleOrder order = new SaleOrder();
        order.setUserId(userId);
        order.setPayAmount(orderTotal);
        order.setPayStatus(0);
        saleOrderMapper.insert(order);

        // 保存订单明细，关联用户购物车中已锁定的批次
        for (int idx = 0; idx < orderItems.size(); idx++) {
            SaleOrderItem oi = orderItems.get(idx);
            OrderItemDTO dto = items.get(idx);
            oi.setOrderId(order.getId());
            saleOrderItemMapper.insert(oi);

            if (dto.getBatchId() == null) {
                throw new BusinessException("下单参数异常：缺少批次ID");
            }

            // 将购物车锁定批次关联到订单明细（批次已由收银台点击时锁定为 status=5）
            int rows = batchDetailMapper.bindToOrderItem(dto.getBatchId(), oi.getId());
            if (rows == 0) {
                // 绑定失败，可能是商品被其他人抢走或状态不对
                ProductBatchDetail current = batchDetailMapper.selectById(dto.getBatchId());
                if (current == null) {
                    throw new BusinessException("商品不存在，请刷新页面");
                }
                if (current.getStatus() != 5) {
                    throw new BusinessException("商品已被他人抢先选中，请刷新后重试");
                }
                throw new BusinessException("商品状态异常，下单失败");
            }
            oi.setBatchDetailId(dto.getBatchId());
            saleOrderItemMapper.updateById(oi);
        }

        return order.getId();
    }

    @Transactional
    public void payOrder(Long orderId, Long userId) {
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getPayStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作他人订单");
        }

        List<SaleOrderItem> items = getOrderItems(orderId);
        order.setPayStatus(1);
        saleOrderMapper.updateById(order);

        for (SaleOrderItem item : items) {
            // 查找该订单明细下已锁定的批次（status=5），而非重新从全库 FIFO 查
            LambdaQueryWrapper<ProductBatchDetail> lockWrapper = new LambdaQueryWrapper<>();
            lockWrapper.eq(ProductBatchDetail::getBelongOrderItemId, item.getId())
                       .eq(ProductBatchDetail::getStatus, 5);
            List<ProductBatchDetail> lockedBatches = batchDetailMapper.selectList(lockWrapper);
            if (lockedBatches.size() < item.getBuyQuantity()) {
                throw new BusinessException("锁定库存不足，支付失败，请取消订单后重新下单");
            }
            for (int i = 0; i < item.getBuyQuantity(); i++) {
                ProductBatchDetail batch = lockedBatches.get(i);
                Long oldShelfId = batch.getShelfId();
                Integer oldLayer = batch.getShelfLayer();
                int rows = batchDetailMapper.markAsSold(batch.getId(), item.getId());
                if (rows == 0) {
                    throw new BusinessException("批次商品状态异常，支付失败");
                }
                // 售出后更新容量追踪：根据是否有货架分配来判断（而非 status 值，因为 status 可能已被改为 5）
                if (oldShelfId != null && oldLayer != null) {
                    capacityTrackingMapper.decrementUsed(oldShelfId, oldLayer, 1);
                }
            }
            if (!lockedBatches.isEmpty()) {
                item.setBatchDetailId(lockedBatches.get(0).getId());
                saleOrderItemMapper.updateById(item);
            }
        }
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getPayStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 解锁所有已锁定批次（status=5 → 0 或 1）
        List<SaleOrderItem> items = getOrderItems(orderId);
        for (SaleOrderItem item : items) {
            LambdaQueryWrapper<ProductBatchDetail> lockWrapper = new LambdaQueryWrapper<>();
            lockWrapper.eq(ProductBatchDetail::getBelongOrderItemId, item.getId())
                       .eq(ProductBatchDetail::getStatus, 5);
            List<ProductBatchDetail> lockedBatches = batchDetailMapper.selectList(lockWrapper);
            for (ProductBatchDetail batch : lockedBatches) {
                batchDetailMapper.markAsUnlocked(batch.getId());
            }
        }

        order.setPayStatus(3);
        saleOrderMapper.updateById(order);
    }

    @Transactional
    public void updatePayStatus(Long orderId, Integer payStatus) {
        if (payStatus == null || !Arrays.asList(0, 1, 2, 3, 4).contains(payStatus)) {
            throw new BusinessException("无效的支付状态");
        }
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setPayStatus(payStatus);
        saleOrderMapper.updateById(order);
    }

    public List<SaleOrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<SaleOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrderItem::getOrderId, orderId);
        return saleOrderItemMapper.selectList(wrapper);
    }

    public Map<String, Object> getOrderDetail(Long orderId) {
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("userId", order.getUserId());
        result.put("payAmount", order.getPayAmount());
        result.put("payStatus", order.getPayStatus());
        result.put("createTime", order.getCreateTime());

        UserInfo user = userInfoMapper.selectById(order.getUserId());
        result.put("username", user != null ? user.getUsername() : "-");

        // 计算实际支付状态（检查是否有部分退款）
        List<SaleOrderItem> items = getOrderItems(orderId);
        boolean hasRefund = false;
        boolean allRefunded = true;
        for (SaleOrderItem si : items) {
            if (si.getRefundQuantity() != null && si.getRefundQuantity() > 0) {
                hasRefund = true;
            }
            if (si.getRefundQuantity() == null || si.getRefundQuantity() < si.getBuyQuantity()) {
                allRefunded = false;
            }
        }
        if (hasRefund && !allRefunded && order.getPayStatus() == 1) {
            result.put("payStatus", 4); // 部分退款
        } else if (allRefunded && hasRefund) {
            result.put("payStatus", 2); // 全部退款
        }

        // 订单详情商品列表 - 每条购买商品都有独立信息
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (SaleOrderItem si : items) {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("id", si.getId());
            im.put("orderId", si.getOrderId());
            im.put("baseProductId", si.getBaseProductId());
            im.put("buyQuantity", si.getBuyQuantity());
            im.put("buyUnitPrice", si.getBuyUnitPrice());
            im.put("itemTotalAmount", si.getItemTotalAmount());
            im.put("refundQuantity", si.getRefundQuantity());
            im.put("batchDetailId", si.getBatchDetailId());

            // 批次信息
            if (si.getBatchDetailId() != null) {
                ProductBatchDetail batch = batchDetailMapper.selectById(si.getBatchDetailId());
                if (batch != null) {
                    im.put("batchShelfDisplayName", batch.getShelfDisplayName());
                    im.put("batchPurchaseUnitPrice", batch.getPurchaseUnitPrice());
                    im.put("batchSalePrice", batch.getSalePrice());
                    im.put("batchDiscountPrice", batch.getDiscountPrice());
                    im.put("batchShelfLayer", batch.getShelfLayer());
                    if (batch.getShelfId() != null) {
                        ShelfInfo shelf = shelfInfoMapper.selectById(batch.getShelfId());
                        im.put("batchShelfCode", shelf != null ? shelf.getShelfCode() : "-");
                    } else {
                        im.put("batchShelfCode", "-");
                    }
                }
            }

            // 商品基础信息
            if (si.getBaseProductId() != null) {
                ProductBase base = productBaseMapper.selectById(si.getBaseProductId());
                im.put("productName", base != null ? base.getProductName() : "-");
                im.put("imageUrl", base != null ? base.getImageUrl() : null);
                im.put("cropData", base != null ? base.getCropData() : null);
            } else {
                im.put("productName", "-");
            }

            // 查询该订单项的退款记录，用于前端展示状态和防重复提交
            List<SaleRefund> refunds = saleRefundMapper.selectByOrderItemId(si.getId());
            int pendingRefundQty = refunds.stream()
                    .filter(r -> r.getRefundHandleStatus() == 0)
                    .mapToInt(r -> r.getRefundQuantity() != null ? r.getRefundQuantity() : 0)
                    .sum();
            im.put("pendingRefundQuantity", pendingRefundQty);
            im.put("hasPendingRefund", pendingRefundQty > 0);
            // 最新一条退款记录的状态（用于快速判断）
            SaleRefund latestRefund = refunds.stream()
                    .max(Comparator.comparing(SaleRefund::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            im.put("latestRefundStatus", latestRefund != null ? latestRefund.getRefundHandleStatus() : null);

            // 查询该订单项关联的所有已售出批次
            List<ProductBatchDetail> soldBatches = batchDetailMapper.selectByOrderItemId(si.getId());
            List<Map<String, Object>> batchItems = new ArrayList<>();
            for (ProductBatchDetail bd : soldBatches) {
                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("batchId", bd.getId());
                bm.put("shelfDisplayName", bd.getShelfDisplayName());
                bm.put("salePrice", bd.getSalePrice());
                bm.put("discountPrice", bd.getDiscountPrice());
                bm.put("purchaseUnitPrice", bd.getPurchaseUnitPrice());
                if (bd.getShelfId() != null) {
                    ShelfInfo shelf = shelfInfoMapper.selectById(bd.getShelfId());
                    bm.put("shelfCode", shelf != null ? shelf.getShelfCode() : "-");
                }
                batchItems.add(bm);
            }
            im.put("batchItems", batchItems);

            itemList.add(im);
        }
        result.put("items", itemList);

        return result;
    }

    /** 更新订单退款状态 */
    @Transactional
    public void updateRefundStatus(Long orderId) {
        List<SaleOrderItem> items = getOrderItems(orderId);
        boolean hasRefund = false;
        boolean allRefunded = true;
        for (SaleOrderItem si : items) {
            if (si.getRefundQuantity() != null && si.getRefundQuantity() > 0) hasRefund = true;
            if (si.getRefundQuantity() == null || si.getRefundQuantity() < si.getBuyQuantity()) allRefunded = false;
        }
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order != null) {
            if (allRefunded && hasRefund) {
                order.setPayStatus(2);
            } else if (hasRefund) {
                order.setPayStatus(4);
            }
            saleOrderMapper.updateById(order);
        }
    }
}
