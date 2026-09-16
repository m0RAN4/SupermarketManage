package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.ShelfCapacityTracking;
import com.supermarket.entity.ShelfInfo;
import com.supermarket.mapper.ShelfCapacityTrackingMapper;
import com.supermarket.mapper.ShelfInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelfInfoService {

    private final ShelfInfoMapper shelfInfoMapper;
    private final ShelfCapacityTrackingMapper capacityTrackingMapper;

    public Page<ShelfInfo> page(Integer current, Integer size, String shelfCode, String shelfArea) {
        Page<ShelfInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<ShelfInfo> wrapper = new LambdaQueryWrapper<>();
        if (shelfCode != null && !shelfCode.trim().isEmpty()) {
            wrapper.like(ShelfInfo::getShelfCode, shelfCode);
        }
        if (shelfArea != null && !shelfArea.trim().isEmpty()) {
            wrapper.like(ShelfInfo::getShelfArea, shelfArea);
        }
        wrapper.orderByAsc(ShelfInfo::getShelfCode);
        return shelfInfoMapper.selectPage(page, wrapper);
    }

    public ShelfInfo getById(Long id) {
        return shelfInfoMapper.selectById(id);
    }

    @Transactional
    public void add(ShelfInfo shelfInfo) {
        if (shelfInfo.getShelfCode() == null || shelfInfo.getShelfCode().trim().isEmpty()) {
            throw new BusinessException("货架编号不能为空");
        }
        LambdaQueryWrapper<ShelfInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfInfo::getShelfCode, shelfInfo.getShelfCode());
        if (shelfInfoMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("货架编号已存在");
        }
        if (shelfInfo.getShelfLayer() != null && shelfInfo.getLayerCapacity() != null) {
            shelfInfo.setTotalCapacity(shelfInfo.getShelfLayer() * shelfInfo.getLayerCapacity());
        }
        shelfInfoMapper.insert(shelfInfo);

        // 为每一层创建容量追踪记录
        if (shelfInfo.getShelfLayer() != null && shelfInfo.getLayerCapacity() != null) {
            for (int layer = 1; layer <= shelfInfo.getShelfLayer(); layer++) {
                ShelfCapacityTracking tracking = new ShelfCapacityTracking();
                tracking.setShelfId(shelfInfo.getId());
                tracking.setShelfLayer(layer);
                tracking.setUsedCount(0);
                tracking.setMaxCapacity(shelfInfo.getLayerCapacity());
                capacityTrackingMapper.insert(tracking);
            }
        }
    }

    @Transactional
    public void update(ShelfInfo shelfInfo) {
        ShelfInfo exist = shelfInfoMapper.selectById(shelfInfo.getId());
        if (exist == null) {
            throw new BusinessException("货架信息不存在");
        }
        LambdaQueryWrapper<ShelfInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfInfo::getShelfCode, shelfInfo.getShelfCode())
               .ne(ShelfInfo::getId, shelfInfo.getId());
        if (shelfInfoMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("货架编号已存在");
        }

        // 先查询当前容量追踪数据（后续同步也用）
        List<ShelfCapacityTracking> trackings = capacityTrackingMapper.selectByShelfId(shelfInfo.getId());

        // 验证新容量是否能容纳已上架商品
        if (shelfInfo.getShelfLayer() != null && shelfInfo.getLayerCapacity() != null) {
            // 检查每层：如果当前已用 > 新容量，拒绝
            for (ShelfCapacityTracking t : trackings) {
                if (t.getShelfLayer() > shelfInfo.getShelfLayer()) {
                    // 该层被删除了，检查是否有商品
                    if (t.getUsedCount() > 0) {
                        throw new BusinessException("调整后货架每层容量不能满足已上架商品数量需求，"
                                + "第" + t.getShelfLayer() + "层有" + t.getUsedCount() + "件商品，"
                                + "请对商品上架情况作出调整后再编辑货架信息");
                    }
                } else if (t.getUsedCount() > shelfInfo.getLayerCapacity()) {
                    throw new BusinessException("调整后货架每层容量不能满足已上架商品数量需求，"
                            + "第" + t.getShelfLayer() + "层容量" + shelfInfo.getLayerCapacity()
                            + "小于已上架" + t.getUsedCount() + "件，"
                            + "请对商品上架情况作出调整后再编辑货架信息");
                }
            }
        }

        shelfInfo.setTotalCapacity(
                shelfInfo.getShelfLayer() != null && shelfInfo.getLayerCapacity() != null
                        ? shelfInfo.getShelfLayer() * shelfInfo.getLayerCapacity() : null);
        shelfInfoMapper.updateById(shelfInfo);

        // 同步容量追踪表
        capacityTrackingMapper.delete(new LambdaQueryWrapper<ShelfCapacityTracking>()
                .eq(ShelfCapacityTracking::getShelfId, shelfInfo.getId()));

        if (shelfInfo.getShelfLayer() != null && shelfInfo.getLayerCapacity() != null) {
            final List<ShelfCapacityTracking> finalTrackings = trackings;
            for (int layer = 1; layer <= shelfInfo.getShelfLayer(); layer++) {
                final int currentLayer = layer;
                ShelfCapacityTracking oldT = finalTrackings != null ? finalTrackings.stream()
                        .filter(t -> t.getShelfLayer() == currentLayer).findFirst().orElse(null) : null;
                ShelfCapacityTracking tracking = new ShelfCapacityTracking();
                tracking.setShelfId(shelfInfo.getId());
                tracking.setShelfLayer(layer);
                tracking.setUsedCount(oldT != null ? Math.min(oldT.getUsedCount(), shelfInfo.getLayerCapacity()) : 0);
                tracking.setMaxCapacity(shelfInfo.getLayerCapacity());
                capacityTrackingMapper.insert(tracking);
            }
        }
    }

    public void toggleEnabled(Long id) {
        ShelfInfo shelfInfo = shelfInfoMapper.selectById(id);
        if (shelfInfo == null) {
            throw new BusinessException("货架信息不存在");
        }
        shelfInfo.setIsEnabled(shelfInfo.getIsEnabled() == 1 ? 0 : 1);
        shelfInfoMapper.updateById(shelfInfo);
    }

    /** 获取货架所有层容量详情 */
    public List<ShelfCapacityTracking> getCapacityDetails(Long shelfId) {
        return capacityTrackingMapper.selectByShelfId(shelfId);
    }
}
