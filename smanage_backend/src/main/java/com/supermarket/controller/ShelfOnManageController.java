package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.entity.ProductBatchDetail;
import com.supermarket.entity.ProductBase;
import com.supermarket.entity.ShelfInfo;
import com.supermarket.mapper.ProductBaseMapper;
import com.supermarket.mapper.ProductBatchDetailMapper;
import com.supermarket.mapper.ShelfInfoMapper;
import com.supermarket.service.ProductBatchDetailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shelf-on")
@RequiredArgsConstructor
public class ShelfOnManageController {

    private final ProductBatchDetailService batchDetailService;
    private final ProductBatchDetailMapper batchDetailMapper;
    private final ProductBaseMapper productBaseMapper;
    private final ShelfInfoMapper shelfInfoMapper;

    @Autowired
    private HttpServletRequest request;

    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /** 列出所有商品及其可上架批次（按商品分组） */
    @GetMapping("/batches")
    public Result<List<Map<String, Object>>> listBatches(
            @RequestParam(required = false) Long baseProductId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String statuses) {
        checkAdmin();
        List<Map<String, Object>> result = new ArrayList<>();

        // 解析多状态参数（逗号分隔）
        List<Integer> statusList = null;
        if (statuses != null && !statuses.trim().isEmpty()) {
            statusList = Arrays.stream(statuses.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
        }

        List<ProductBase> bases;
        if (baseProductId != null) {
            ProductBase pb = productBaseMapper.selectById(baseProductId);
            bases = pb != null ? List.of(pb) : List.of();
        } else {
            bases = productBaseMapper.selectList(null);
        }

        for (ProductBase base : bases) {
            List<ProductBatchDetail> batches;
            if (statusList != null && !statusList.isEmpty()) {
                batches = batchDetailService.listByProductAndStatuses(base.getId(), statusList);
            } else if (status != null) {
                batches = batchDetailService.listByProductAndStatus(base.getId(), status);
            } else {
                batches = batchDetailService.listByBaseProductId(base.getId());
            }

            if (batches.isEmpty()) continue;

            Map<String, Object> group = new LinkedHashMap<>();
            group.put("baseProductId", base.getId());
            group.put("productName", base.getProductName());
            group.put("categoryId", base.getCategoryId());

            Map<Integer, Long> statusCounts = batchDetailService.countGroupByStatus(base.getId());
            group.put("totalCount", batches.size());
            group.put("onShelfCount", statusCounts.getOrDefault(1, 0L));
            group.put("warehouseCount", statusCounts.getOrDefault(0, 0L));

            List<Map<String, Object>> batchList = new ArrayList<>();
            for (ProductBatchDetail b : batches) {
                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("id", b.getId());
                bm.put("purchaseRecordId", b.getPurchaseRecordId());
                bm.put("shelfId", b.getShelfId());
                bm.put("shelfLayer", b.getShelfLayer());
                bm.put("shelfDisplayName", b.getShelfDisplayName());
                bm.put("purchaseUnitPrice", b.getPurchaseUnitPrice());
                bm.put("salePrice", b.getSalePrice());
                bm.put("discountPrice", b.getDiscountPrice());
                bm.put("status", b.getStatus());
                bm.put("createTime", b.getCreateTime());
                if (b.getShelfId() != null) {
                    ShelfInfo shelf = shelfInfoMapper.selectById(b.getShelfId());
                    bm.put("shelfCode", shelf != null ? shelf.getShelfCode() : "-");
                }
                batchList.add(bm);
            }
            group.put("batches", batchList);
            result.add(group);
        }

        return Result.ok(result);
    }

    /** 获取货架各层占用情况 */
    @GetMapping("/shelf-layers/{shelfId}")
    public Result<Map<String, Object>> getShelfLayers(@PathVariable Long shelfId) {
        checkAdmin();
        ShelfInfo shelf = shelfInfoMapper.selectById(shelfId);
        if (shelf == null) {
            throw new BusinessException("货架不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shelfId", shelf.getId());
        result.put("shelfCode", shelf.getShelfCode());
        result.put("shelfLayer", shelf.getShelfLayer());
        result.put("layerCapacity", shelf.getLayerCapacity());
        result.put("totalCapacity", shelf.getTotalCapacity());

        Map<Integer, Long> layerCounts = batchDetailService.countByShelfAndLayer(shelfId);
        List<Map<String, Object>> layers = new ArrayList<>();
        if (shelf.getShelfLayer() != null) {
            for (int i = 1; i <= shelf.getShelfLayer(); i++) {
                Map<String, Object> layer = new LinkedHashMap<>();
                layer.put("layer", i);
                long used = layerCounts.getOrDefault(i, 0L);
                layer.put("used", used);
                layer.put("capacity", shelf.getLayerCapacity() != null ? shelf.getLayerCapacity() : 0);
                layer.put("available", shelf.getLayerCapacity() != null
                        ? Math.max(0, shelf.getLayerCapacity() - used) : 0);
                layers.add(layer);
            }
        }
        result.put("layers", layers);

        long totalUsed = layerCounts.values().stream().mapToLong(Long::longValue).sum();
        result.put("totalUsed", totalUsed);
        result.put("totalAvailable", shelf.getTotalCapacity() != null
                ? Math.max(0, shelf.getTotalCapacity() - totalUsed) : 0);

        return Result.ok(result);
    }

    /** 批量上架（支持自动分配货架和容量确认） */
    @PostMapping("/batch-on")
    public Result<?> batchShelfOn(@RequestBody Map<String, Object> body) {
        checkAdmin();
        @SuppressWarnings("unchecked")
        List<Long> batchIds = ((List<Number>) body.get("batchIds")).stream()
                .map(Number::longValue).collect(Collectors.toList());
        Long shelfId = body.get("shelfId") != null
                ? Long.valueOf(body.get("shelfId").toString()) : null;
        Integer shelfLayer = body.get("shelfLayer") != null
                ? Integer.valueOf(body.get("shelfLayer").toString()) : null;
        String shelfDisplayName = body.get("shelfDisplayName") != null
                ? body.get("shelfDisplayName").toString() : null;
        BigDecimal salePrice = body.get("salePrice") != null
                ? new BigDecimal(body.get("salePrice").toString()) : null;
        BigDecimal discountPrice = body.get("discountPrice") != null
                ? new BigDecimal(body.get("discountPrice").toString()) : null;
        boolean forceContinue = body.get("forceContinue") != null
                && Boolean.parseBoolean(body.get("forceContinue").toString());

        Map<String, Object> assignResult = batchDetailService.batchShelfOnWithAssign(
                batchIds, shelfId, shelfLayer, shelfDisplayName, salePrice, discountPrice, forceContinue);

        // 如果需要确认，返回提示信息
        if (Boolean.TRUE.equals(assignResult.get("needConfirm"))) {
            return Result.ok(assignResult);
        }

        // 上架完成，返回结果
        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) assignResult.get("warnings");
        int unplaced = (int) assignResult.getOrDefault("unplacedCount", 0);
        int placed = (int) assignResult.getOrDefault("totalPlaced", 0);

        String msg = "上架成功，共上架" + placed + "件";
        if (unplaced > 0) msg += "，" + unplaced + "件未能上架（容量不足）";
        if (warnings != null && !warnings.isEmpty()) msg += "。" + String.join("；", warnings);

        assignResult.put("message", msg);
        return Result.ok(assignResult);
    }

    /** 批量下架 */
    @PostMapping("/batch-off")
    public Result<?> batchShelfOff(@RequestBody Map<String, Object> body) {
        checkAdmin();
        @SuppressWarnings("unchecked")
        List<Long> batchIds = ((List<Number>) body.get("batchIds")).stream()
                .map(Number::longValue).collect(Collectors.toList());
        batchDetailService.batchShelfOff(batchIds);
        return Result.ok("下架成功");
    }

    /** 批量改价 */
    @PutMapping("/price")
    public Result<?> batchUpdatePrice(@RequestBody Map<String, Object> body) {
        checkAdmin();
        @SuppressWarnings("unchecked")
        List<Long> batchIds = ((List<Number>) body.get("batchIds")).stream()
                .map(Number::longValue).collect(Collectors.toList());
        BigDecimal salePrice = body.get("salePrice") != null
                ? new BigDecimal(body.get("salePrice").toString()) : null;
        BigDecimal discountPrice = body.get("discountPrice") != null
                ? new BigDecimal(body.get("discountPrice").toString()) : null;

        batchDetailService.batchUpdatePrice(batchIds, salePrice, discountPrice);
        return Result.ok("改价成功");
    }

    /** 收银台用 - 按货架分组查询在售商品 */
    @GetMapping("/for-sale")
    public Result<List<Map<String, Object>>> listForSale() {
        List<Map<String, Object>> result = new ArrayList<>();

        List<ShelfInfo> enabledShelves = shelfInfoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShelfInfo>()
                        .eq(ShelfInfo::getIsEnabled, 1)
                        .orderByAsc(ShelfInfo::getShelfCode));

        for (ShelfInfo shelf : enabledShelves) {
            Map<String, Object> shelfGroup = new LinkedHashMap<>();
            shelfGroup.put("shelfId", shelf.getId());
            shelfGroup.put("shelfCode", shelf.getShelfCode());
            shelfGroup.put("shelfArea", shelf.getShelfArea());
            shelfGroup.put("shelfLayer", shelf.getShelfLayer());

            List<ProductBatchDetail> onShelfItems = batchDetailService.listByProductAndStatus(null, 1);
            onShelfItems = onShelfItems.stream()
                    .filter(b -> shelf.getId().equals(b.getShelfId()))
                    .collect(Collectors.toList());

            // 按层分组
            Map<Integer, List<Map<String, Object>>> layerGroups = new LinkedHashMap<>();
            for (ProductBatchDetail b : onShelfItems) {
                int layer = b.getShelfLayer() != null ? b.getShelfLayer() : 0;
                layerGroups.computeIfAbsent(layer, k -> new ArrayList<>());

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("batchId", b.getId());
                item.put("baseProductId", b.getBaseProductId());
                item.put("shelfDisplayName", b.getShelfDisplayName());
                item.put("salePrice", b.getSalePrice());
                item.put("discountPrice", b.getDiscountPrice());

                ProductBase base = productBaseMapper.selectById(b.getBaseProductId());
                item.put("productName", base != null ? base.getProductName() : "-");
                item.put("imageUrl", base != null ? base.getImageUrl() : null);
                item.put("cropData", base != null ? base.getCropData() : null);
                layerGroups.get(layer).add(item);
            }

            List<Map<String, Object>> layers = new ArrayList<>();
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : layerGroups.entrySet()) {
                Map<String, Object> layerData = new LinkedHashMap<>();
                layerData.put("layer", entry.getKey());
                layerData.put("items", entry.getValue());
                layerData.put("itemCount", entry.getValue().size());
                layers.add(layerData);
            }
            shelfGroup.put("layers", layers);
            shelfGroup.put("totalItems", onShelfItems.size());
            result.add(shelfGroup);
        }

        return Result.ok(result);
    }

    /** 收银台点击商品加入购物车：立即锁定该批次（1 → 5），从货架移除 */
    @PostMapping("/reserve/{batchId}")
    public Result<?> reserveForCart(@PathVariable Long batchId) {
        int rows = batchDetailMapper.reserveForCart(batchId);
        if (rows == 0) {
            throw new BusinessException("该商品已被他人抢先选中，请刷新页面");
        }
        return Result.ok("已锁定");
    }

    /** 收银台从购物车移除商品：解锁该批次（5 → 1），重新上架 */
    @PostMapping("/release/{batchId}")
    public Result<?> releaseFromCart(@PathVariable Long batchId) {
        int rows = batchDetailMapper.releaseFromCart(batchId);
        if (rows == 0) {
            throw new BusinessException("解锁失败，商品状态异常");
        }
        return Result.ok("已释放");
    }

    /** 收银台用 - 原有简单列表接口（兼容） */
    @GetMapping("/list-valid")
    public Result<List<Map<String, Object>>> listValid() {
        List<ProductBatchDetail> onShelf = batchDetailService.listByProductAndStatus(null, 1);
        // 按 baseProductId 去重
        Map<Long, List<ProductBatchDetail>> grouped = onShelf.stream()
                .collect(Collectors.groupingBy(ProductBatchDetail::getBaseProductId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<ProductBatchDetail>> entry : grouped.entrySet()) {
            ProductBase base = productBaseMapper.selectById(entry.getKey());
            if (base == null || base.getIsEnabled() != 1) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("baseProductId", entry.getKey());
            item.put("productName", base.getProductName());
            item.put("imageUrl", base.getImageUrl());
            item.put("cropData", base.getCropData());

            ProductBatchDetail sample = entry.getValue().get(0);
            item.put("shelfDisplayName", sample.getShelfDisplayName());
            item.put("salePrice", sample.getSalePrice());
            item.put("discountPrice", sample.getDiscountPrice());
            item.put("onShelfQuantity", entry.getValue().size());
            if (sample.getShelfId() != null) {
                ShelfInfo shelf = shelfInfoMapper.selectById(sample.getShelfId());
                item.put("shelfCode", shelf != null ? shelf.getShelfCode() : "-");
            }
            result.add(item);
        }
        return Result.ok(result);
    }
}
