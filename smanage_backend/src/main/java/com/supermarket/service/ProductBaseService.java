package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.ProductBase;
import com.supermarket.entity.ProductCategory;
import com.supermarket.mapper.ProductBaseMapper;
import com.supermarket.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductBaseService {

    private final ProductBaseMapper productBaseMapper;
    private final ProductCategoryMapper productCategoryMapper;

    public Page<ProductBase> page(Integer current, Integer size, String keyword, Long categoryId) {
        Page<ProductBase> page = new Page<>(current, size);
        LambdaQueryWrapper<ProductBase> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(ProductBase::getProductName, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(ProductBase::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(ProductBase::getCreateTime);
        return productBaseMapper.selectPage(page, wrapper);
    }

    public ProductBase getById(Long id) {
        return productBaseMapper.selectById(id);
    }

    public void add(ProductBase productBase) {
        if (productBase.getProductName() == null || productBase.getProductName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (productBase.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(productBase.getCategoryId());
            if (category == null) {
                throw new BusinessException("商品分类不存在");
            }
        }
        productBaseMapper.insert(productBase);
    }

    public void update(ProductBase productBase) {
        ProductBase exist = productBaseMapper.selectById(productBase.getId());
        if (exist == null) {
            throw new BusinessException("商品信息不存在");
        }
        if (productBase.getProductName() == null || productBase.getProductName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (productBase.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(productBase.getCategoryId());
            if (category == null) {
                throw new BusinessException("商品分类不存在");
            }
        }
        productBaseMapper.updateById(productBase);
    }

    public void toggleEnabled(Long id) {
        ProductBase productBase = productBaseMapper.selectById(id);
        if (productBase == null) {
            throw new BusinessException("商品信息不存在");
        }
        productBase.setIsEnabled(productBase.getIsEnabled() == 1 ? 0 : 1);
        productBaseMapper.updateById(productBase);
    }
}
