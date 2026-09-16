package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.ProductBatchDetail;
import com.supermarket.entity.ShelfCapacityTracking;
import com.supermarket.entity.ShelfInfo;
import com.supermarket.mapper.ProductBatchDetailMapper;
import com.supermarket.mapper.ShelfCapacityTrackingMapper;
import com.supermarket.mapper.ShelfInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBatchDetailService {

    private final ProductBatchDetailMapper batchDetailMapper;
    private final ShelfInfoMapper shelfInfoMapper;
    private final ShelfCapacityTrackingMapper capacityTrackingMapper;

    public List<ProductBatchDetail> listByBaseProductId(Long baseProductId) {
        return batchDetailMapper.selectByBaseProductId(baseProductId);
    }

    public List<ProductBatchDetail> listByProductAndStatus(Long baseProductId, Integer status) {
        if (baseProductId == null) {
            // 查询所有指定状态的批次
            LambdaQueryWrapper<ProductBatchDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductBatchDetail::getStatus, status);
            return batchDetailMapper.selectList(wrapper);
        }
        return batchDetailMapper.selectByProductAndStatus(baseProductId, status);
    }

    /** 根据商品ID和多个状态查询批次 */
    public List<ProductBatchDetail> listByProductAndStatuses(Long baseProductId, List<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return listByBaseProductId(baseProductId);
        }
        if (baseProductId == null) {
            LambdaQueryWrapper<ProductBatchDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ProductBatchDetail::getStatus, statuses);
            return batchDetailMapper.selectList(wrapper);
        }
        return batchDetailMapper.selectByProductAndStatuses(baseProductId, statuses);
    }

    public Map<Integer, Long> countGroupByStatus(Long baseProductId) {
        List<Map<String, Object>> rows = batchDetailMapper.countGroupByStatus(baseProductId);
        Map<Integer, Long> result = new HashMap<>();
        for (int s = 0; s <= 4; s++) {
            result.put(s, 0L);
        }
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object statusObj = row.get("status");
                Object cntObj = row.get("cnt");
                if (statusObj != null && cntObj != null) {
                    result.put(Integer.valueOf(statusObj.toString()), Long.valueOf(cntObj.toString()));
                }
            }
        }
        return result;
    }

    /** 按货架统计各层已占用数量（从 tracking 表读取） */
    public Map<Integer, Long> countByShelfAndLayer(Long shelfId) {
        List<ShelfCapacityTracking> trackings = capacityTrackingMapper.selectByShelfId(shelfId);
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (ShelfCapacityTracking t : trackings) {
            result.put(t.getShelfLayer(), (long) t.getUsedCount());
        }
        return result;
    }

    /**
     * 自动分配上架位置
     * @return Map: "placed" → List of placed batch IDs, "unplacedCount" → count of unplaced items,
     *         "warnings" → list of warning messages
     */
    public Map<String, Object> autoAssignShelf(List<Long> batchDetailIds, Long startShelfId,
                                                Integer startLayer, String shelfDisplayName,
                                                BigDecimal salePrice, BigDecimal discountPrice) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Long> placed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int remaining = batchDetailIds.size();

        // 获取所有启用的货架，按编号排序
        List<ShelfInfo> allShelves = shelfInfoMapper.selectList(
                new LambdaQueryWrapper<ShelfInfo>()
                        .eq(ShelfInfo::getIsEnabled, 1)
                        .orderByAsc(ShelfInfo::getShelfCode));

        if (allShelves.isEmpty()) {
            throw new BusinessException("没有可用的货架");
        }

        // 计算总剩余容量
        int totalRemaining = 0;
        for (ShelfInfo s : allShelves) {
            Integer shelfRemaining = capacityTrackingMapper.selectShelfRemaining(s.getId());
            totalRemaining += (shelfRemaining != null ? shelfRemaining : 0);
        }

        // 阶段一：如果指定了货架，优先在该货架内顺延（从指定层开始，填满该层后继续向该货架其他层顺延）
        if (startShelfId != null) {
            int startShelfIdx = -1;
            for (int i = 0; i < allShelves.size(); i++) {
                if (allShelves.get(i).getId().equals(startShelfId)) {
                    startShelfIdx = i;
                    break;
                }
            }
            if (startShelfIdx >= 0) {
                ShelfInfo startShelf = allShelves.get(startShelfIdx);
                if (startShelf.getShelfLayer() != null && startShelf.getLayerCapacity() != null) {
                    int startL = startLayer != null ? startLayer : 1;
                    remaining = fillShelfLayers(startShelf, startL, remaining, batchDetailIds, placed,
                            shelfDisplayName, salePrice, discountPrice);
                }
            }
        }

        // 阶段二：指定货架填满后，从第一个货架开始逐货架逐层顺延
        if (remaining > 0) {
            for (int si = 0; si < allShelves.size() && remaining > 0; si++) {
                ShelfInfo shelf = allShelves.get(si);
                if (shelf.getShelfLayer() == null || shelf.getLayerCapacity() == null) continue;
                remaining = fillShelfLayers(shelf, 1, remaining, batchDetailIds, placed,
                        shelfDisplayName, salePrice, discountPrice);
            }
        }

        // 生成警告信息
        if (remaining > 0 && totalRemaining < batchDetailIds.size()) {
            warnings.add("超市货架总剩余容量(" + totalRemaining + ")不足以容纳全部商品(" + batchDetailIds.size() + "件)，"
                    + remaining + "件商品无法上架，将继续保留在仓库");
        } else if (remaining > 0 && placed.size() > 0) {
            warnings.add("指定位置已放满，" + remaining + "件商品已顺延至其他货架");
        }

        result.put("placed", placed);
        result.put("unplacedCount", remaining);
        result.put("totalPlaced", placed.size());
        result.put("warnings", warnings);
        return result;
    }

    /** 在指定货架上从某层开始逐层填充，返回剩余未放置数量 */
    private int fillShelfLayers(ShelfInfo shelf, int startLayer, int remaining,
                                 List<Long> batchDetailIds, List<Long> placed,
                                 String shelfDisplayName, BigDecimal salePrice, BigDecimal discountPrice) {
        for (int layer = startLayer; layer <= shelf.getShelfLayer() && remaining > 0; layer++) {
            ShelfCapacityTracking tracking = capacityTrackingMapper.selectByShelfAndLayer(shelf.getId(), layer);
            int available = tracking != null ? tracking.getMaxCapacity() - tracking.getUsedCount() : 0;
            if (available <= 0) continue;

            int toPlace = Math.min(available, remaining);

            // 执行上架（batchDetailIds 已经过预过滤，均为可上架状态）
            for (int i = 0; i < toPlace; i++) {
                Long batchId = batchDetailIds.get(placed.size());
                batchDetailMapper.shelfOn(batchId, shelf.getId(), layer,
                        shelfDisplayName, salePrice, discountPrice);
                placed.add(batchId);
            }

            // 更新容量追踪
            capacityTrackingMapper.incrementUsed(shelf.getId(), layer, toPlace);

            remaining -= toPlace;
        }
        return remaining;
    }

    /** 单条上架（指定位置，带容量追踪更新） */
    @Transactional
    public void shelfOn(Long batchDetailId, Long shelfId, Integer shelfLayer,
                        String shelfDisplayName, BigDecimal salePrice, BigDecimal discountPrice) {
        ProductBatchDetail detail = batchDetailMapper.selectById(batchDetailId);
        if (detail == null) throw new BusinessException("批次明细不存在");
        if (detail.getStatus() != 0) throw new BusinessException("该批次商品不是未上架状态，无法上架");
        if (shelfId == null) throw new BusinessException("请选择目标货架");

        ShelfInfo shelf = shelfInfoMapper.selectById(shelfId);
        if (shelf == null) throw new BusinessException("货架不存在");
        if (shelf.getIsEnabled() != 1) throw new BusinessException("该货架已被禁用");

        if (shelfLayer != null && shelf.getShelfLayer() != null && shelfLayer > shelf.getShelfLayer()) {
            throw new BusinessException("层数超出货架总层数（" + shelf.getShelfLayer() + "）");
        }

        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("出售价格必须大于0");
        }
        if (discountPrice != null && discountPrice.compareTo(salePrice) > 0) {
            throw new BusinessException("折扣价格不能大于出售价格");
        }

        int rows = batchDetailMapper.shelfOn(batchDetailId, shelfId, shelfLayer,
                shelfDisplayName != null ? shelfDisplayName.trim() : null, salePrice, discountPrice);
        if (rows == 0) throw new BusinessException("上架操作失败");

        // 更新容量追踪
        if (shelfLayer != null) {
            capacityTrackingMapper.incrementUsed(shelfId, shelfLayer, 1);
        }
    }

    /** 批量上架（支持自动分配，自动跳过非在库状态的批次） */
    @Transactional
    public Map<String, Object> batchShelfOnWithAssign(List<Long> batchDetailIds, Long shelfId,
                                                       Integer shelfLayer, String shelfDisplayName,
                                                       BigDecimal salePrice, BigDecimal discountPrice,
                                                       boolean forceContinue) {
        if (batchDetailIds == null || batchDetailIds.isEmpty()) {
            throw new BusinessException("请选择要上架的批次");
        }

        // 价格校验
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("出售价格必须大于0");
        }
        if (discountPrice != null && discountPrice.compareTo(salePrice) > 0) {
            throw new BusinessException("折扣价格不能大于出售价格");
        }

        // 过滤：只保留状态为「在库未上架」(0)的批次
        List<Long> validIds = new ArrayList<>();
        int skipped = 0;
        for (Long id : batchDetailIds) {
            ProductBatchDetail detail = batchDetailMapper.selectById(id);
            if (detail != null && detail.getStatus() == 0) {
                validIds.add(id);
            } else {
                skipped++;
            }
        }
        if (validIds.isEmpty()) {
            throw new BusinessException("所选批次中没有可上架的商品（状态必须为 在库未上架 ）");
        }

        // 使用有效批次数量进行容量检查
        int totalRequested = validIds.size();

        // 如果指定了货架和层数，先检查该位置容量
        if (shelfId != null && shelfLayer != null) {
            ShelfCapacityTracking tracking = capacityTrackingMapper.selectByShelfAndLayer(shelfId, shelfLayer);
            int available = tracking != null ? tracking.getMaxCapacity() - tracking.getUsedCount() : 0;
            if (available < totalRequested && !forceContinue) {
                Map<String, Object> hint = new LinkedHashMap<>();
                hint.put("needConfirm", true);
                hint.put("available", available);
                hint.put("requested", totalRequested);
                hint.put("message", "第" + shelfLayer + "层仅剩" + available + "个空位，"
                        + (totalRequested - available) + "件超出。超出部分将优先在本货架其他层顺延，本货架填满后从第一个货架开始。");
                hint.put("totalRemaining", capacityTrackingMapper.selectTotalRemaining());
                return hint;
            }
        } else if (shelfId != null) {
            Integer shelfRemaining = capacityTrackingMapper.selectShelfRemaining(shelfId);
            if (shelfRemaining != null && shelfRemaining < totalRequested && !forceContinue) {
                Map<String, Object> hint = new LinkedHashMap<>();
                hint.put("needConfirm", true);
                hint.put("available", shelfRemaining);
                hint.put("requested", totalRequested);
                hint.put("message", "该货架仅剩" + shelfRemaining + "个空位，"
                        + (totalRequested - shelfRemaining) + "件超出。是否顺延至其他货架？");
                hint.put("totalRemaining", capacityTrackingMapper.selectTotalRemaining());
                return hint;
            }
        } else {
            // 没有指定货架，自动从第一个开始
            Integer totalRemaining = capacityTrackingMapper.selectTotalRemaining();
            if (totalRemaining != null && totalRemaining < totalRequested && !forceContinue) {
                Map<String, Object> hint = new LinkedHashMap<>();
                hint.put("needConfirm", true);
                hint.put("available", totalRemaining);
                hint.put("requested", totalRequested);
                hint.put("message", "超市所有货架总剩余容量(" + totalRemaining + ")不足以容纳全部"
                        + totalRequested + "件商品，" + (totalRequested - totalRemaining)
                        + "件超出部分将不上架。是否确定按顺序上架？");
                hint.put("totalRemaining", totalRemaining);
                return hint;
            }
        }

        // 执行自动分配
        Map<String, Object> assignResult = autoAssignShelf(validIds, shelfId, shelfLayer,
                shelfDisplayName, salePrice, discountPrice);

        // 追加跳过提示
        if (skipped > 0) {
            @SuppressWarnings("unchecked")
            List<String> warnings = (List<String>) assignResult.get("warnings");
            if (warnings == null) {
                warnings = new ArrayList<>();
                assignResult.put("warnings", warnings);
            }
            warnings.add("已自动跳过" + skipped + "个非在库状态的批次");
        }

        return assignResult;
    }

    /** 批量下架（更新容量追踪，自动跳过非已上架状态的批次） */
    @Transactional
    public void batchShelfOff(List<Long> batchDetailIds) {
        if (batchDetailIds == null || batchDetailIds.isEmpty()) {
            throw new BusinessException("请选择要下架的批次");
        }
        int done = 0, skipped = 0;
        for (Long id : batchDetailIds) {
            ProductBatchDetail detail = batchDetailMapper.selectById(id);
            if (detail == null || detail.getStatus() != 1) {
                skipped++;
                continue;
            }
            // 先记下货架和层数
            Long shelfId = detail.getShelfId();
            Integer layer = detail.getShelfLayer();
            batchDetailMapper.shelfOff(id);
            // 更新容量追踪
            if (shelfId != null && layer != null) {
                capacityTrackingMapper.decrementUsed(shelfId, layer, 1);
            }
            done++;
        }
        if (done == 0) {
            throw new BusinessException("所选批次中没有可下架的商品（状态必须为 已上架在售 ）");
        }
    }

    /** 批量改价 */
    @Transactional
    public void batchUpdatePrice(List<Long> batchDetailIds, BigDecimal salePrice, BigDecimal discountPrice) {
        if (batchDetailIds == null || batchDetailIds.isEmpty()) {
            throw new BusinessException("请选择要改价的批次");
        }
        for (Long id : batchDetailIds) {
            batchDetailMapper.updatePrice(id, salePrice, discountPrice);
        }
    }

}
