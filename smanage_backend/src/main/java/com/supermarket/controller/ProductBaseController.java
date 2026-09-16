package com.supermarket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.entity.ProductBase;
import com.supermarket.entity.ProductBatchDetail;
import com.supermarket.entity.ProductCategory;
import com.supermarket.entity.ShelfInfo;
import com.supermarket.mapper.ProductBaseMapper;
import com.supermarket.mapper.ProductBatchDetailMapper;
import com.supermarket.mapper.ProductCategoryMapper;
import com.supermarket.mapper.ShelfInfoMapper;
import com.supermarket.service.ProductBaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/product-base")
@RequiredArgsConstructor
public class ProductBaseController {

    private final ProductBaseService productBaseService;
    private final ProductBaseMapper productBaseMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductBatchDetailMapper batchDetailMapper;
    private final ShelfInfoMapper shelfInfoMapper;

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
     * 分页查询商品基础列表（附加分类名称）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        checkAdmin();
        Page<ProductBase> page = productBaseService.page(current, size, keyword, categoryId);

        // 注入分类名称
        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (ProductBase pb : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", pb.getId());
            item.put("categoryId", pb.getCategoryId());
            item.put("productName", pb.getProductName());
            item.put("imageUrl", pb.getImageUrl());
            item.put("cropData", pb.getCropData());
            item.put("isEnabled", pb.getIsEnabled());
            item.put("createTime", pb.getCreateTime());
            item.put("updateTime", pb.getUpdateTime());

            ProductCategory category = productCategoryMapper.selectById(pb.getCategoryId());
            item.put("categoryName", category != null ? category.getCategoryName() : null);
            enrichedRecords.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        result.put("pages", page.getPages());
        return Result.ok(result);
    }

    /**
     * 根据ID查询商品基础信息
     */
    @GetMapping("/{id}")
    public Result<ProductBase> getById(@PathVariable Long id) {
        checkAdmin();
        ProductBase productBase = productBaseService.getById(id);
        return Result.ok(productBase);
    }

    /**
     * 查询已启用的商品基础列表（可选按分类过滤）
     */
    @GetMapping("/list")
    public Result<List<ProductBase>> list(@RequestParam(required = false) Long categoryId) {
        checkAdmin();
        LambdaQueryWrapper<ProductBase> wrapper = new LambdaQueryWrapper<ProductBase>()
                .eq(ProductBase::getIsEnabled, 1);
        if (categoryId != null) {
            wrapper.eq(ProductBase::getCategoryId, categoryId);
        }
        List<ProductBase> list = productBaseMapper.selectList(wrapper);
        return Result.ok(list);
    }

    /**
     * 查询商品的库存明细（所有批次，包含货架编号等完整信息，用于损耗管理等场景）
     */
    @GetMapping("/stock-detail/{baseProductId}")
    public Result<List<Map<String, Object>>> stockDetail(@PathVariable Long baseProductId) {
        checkAdmin();
        List<ProductBatchDetail> details = batchDetailMapper.selectByBaseProductId(baseProductId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductBatchDetail b : details) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", b.getId());
            item.put("purchaseRecordId", b.getPurchaseRecordId());
            item.put("baseProductId", b.getBaseProductId());
            item.put("shelfId", b.getShelfId());
            item.put("shelfLayer", b.getShelfLayer());
            item.put("shelfDisplayName", b.getShelfDisplayName());
            item.put("purchaseUnitPrice", b.getPurchaseUnitPrice());
            item.put("salePrice", b.getSalePrice());
            item.put("discountPrice", b.getDiscountPrice());
            item.put("status", b.getStatus());
            item.put("createTime", b.getCreateTime());
            item.put("updateTime", b.getUpdateTime());
            if (b.getShelfId() != null) {
                ShelfInfo shelf = shelfInfoMapper.selectById(b.getShelfId());
                item.put("shelfCode", shelf != null ? shelf.getShelfCode() : "-");
            } else {
                item.put("shelfCode", "-");
            }
            result.add(item);
        }
        return Result.ok(result);
    }

    /**
     * 添加商品基础信息
     */
    @PostMapping
    public Result<?> add(@RequestBody ProductBase productBase) {
        checkAdmin();
        productBaseService.add(productBase);
        return Result.ok("添加成功");
    }

    /**
     * 更新商品基础信息
     */
    @PutMapping
    public Result<?> update(@RequestBody ProductBase productBase) {
        checkAdmin();
        productBaseService.update(productBase);
        return Result.ok("更新成功");
    }

    /**
     * 切换商品基础启用/禁用状态
     */
    @PutMapping("/toggle/{id}")
    public Result<?> toggleEnabled(@PathVariable Long id) {
        checkAdmin();
        productBaseService.toggleEnabled(id);
        return Result.ok();
    }
}
