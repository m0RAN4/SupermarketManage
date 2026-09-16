package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.PromotionImage;
import com.supermarket.mapper.PromotionImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionImageService {

    private final PromotionImageMapper promotionImageMapper;

    /**
     * 分页查询宣传图片（管理端用）
     */
    public Page<PromotionImage> page(Integer current, Integer size) {
        Page<PromotionImage> page = new Page<>(current, size);
        LambdaQueryWrapper<PromotionImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PromotionImage::getCreateTime);
        return promotionImageMapper.selectPage(page, wrapper);
    }

    /**
     * 查询所有启用的宣传图片（用户端用）
     */
    public List<PromotionImage> listActive() {
        LambdaQueryWrapper<PromotionImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionImage::getIsActive, 1)
               .orderByDesc(PromotionImage::getCreateTime);
        return promotionImageMapper.selectList(wrapper);
    }

    /**
     * 添加宣传图片
     */
    public void add(PromotionImage image) {
        if (image.getImageUrl() == null || image.getImageUrl().trim().isEmpty()) {
            throw new BusinessException("图片URL不能为空");
        }
        image.setIsActive(1);
        promotionImageMapper.insert(image);
    }

    /**
     * 切换启用/禁用状态
     */
    public void toggleActive(Long id) {
        PromotionImage image = promotionImageMapper.selectById(id);
        if (image == null) {
            throw new BusinessException("宣传图片不存在");
        }
        image.setIsActive(image.getIsActive() == 1 ? 0 : 1);
        promotionImageMapper.updateById(image);
    }

    /**
     * 软删除宣传图片（MP自动将 is_deleted 设为 1，后续查询自动过滤）
     */
    public void softDelete(Long id) {
        PromotionImage image = promotionImageMapper.selectById(id);
        if (image == null) {
            throw new BusinessException("宣传图片不存在");
        }
        promotionImageMapper.deleteById(id);
    }
}
