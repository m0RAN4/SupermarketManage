package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.ProductCategory;
import com.supermarket.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;

    public Page<ProductCategory> page(Integer current, Integer size, String keyword) {
        Page<ProductCategory> page = new Page<>(current, size);
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(ProductCategory::getCategoryName, keyword);
        }
        wrapper.orderByAsc(ProductCategory::getSort);
        return productCategoryMapper.selectPage(page, wrapper);
    }

    public ProductCategory getById(Long id) {
        return productCategoryMapper.selectById(id);
    }

    public void add(ProductCategory category) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getCategoryName, category.getCategoryName());
        if (productCategoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        productCategoryMapper.insert(category);
    }

    public void update(ProductCategory category) {
        ProductCategory exist = productCategoryMapper.selectById(category.getId());
        if (exist == null) {
            throw new BusinessException("分类不存在");
        }
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getCategoryName, category.getCategoryName())
               .ne(ProductCategory::getId, category.getId());
        if (productCategoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        productCategoryMapper.updateById(category);
    }

    public void toggleEnabled(Long id) {
        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        category.setIsEnabled(category.getIsEnabled() == 1 ? 0 : 1);
        productCategoryMapper.updateById(category);
    }
}
