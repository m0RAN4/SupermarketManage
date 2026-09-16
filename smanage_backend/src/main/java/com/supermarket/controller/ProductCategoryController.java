package com.supermarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.entity.ProductCategory;
import com.supermarket.service.ProductCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @Autowired
    private HttpServletRequest request;

    /**
     * 校验管理员权限
     */
    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /**
     * 分页查询分类列表
     */
    @GetMapping("/page")
    public Result<Page<ProductCategory>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        checkAdmin();
        Page<ProductCategory> page = productCategoryService.page(current, size, keyword);
        return Result.ok(page);
    }

    /**
     * 根据ID查询分类
     */
    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable Long id) {
        checkAdmin();
        ProductCategory category = productCategoryService.getById(id);
        return Result.ok(category);
    }

    /**
     * 添加分类
     */
    @PostMapping
    public Result<?> add(@RequestBody ProductCategory category) {
        checkAdmin();
        productCategoryService.add(category);
        return Result.ok("添加成功");
    }

    /**
     * 更新分类
     */
    @PutMapping
    public Result<?> update(@RequestBody ProductCategory category) {
        checkAdmin();
        productCategoryService.update(category);
        return Result.ok("更新成功");
    }

    /**
     * 切换分类启用/禁用状态
     */
    @PutMapping("/toggle/{id}")
    public Result<?> toggleEnabled(@PathVariable Long id) {
        checkAdmin();
        productCategoryService.toggleEnabled(id);
        return Result.ok();
    }
}
