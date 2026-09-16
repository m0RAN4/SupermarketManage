package com.supermarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.entity.PromotionImage;
import com.supermarket.service.PromotionImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class PromotionImageController {

    private final PromotionImageService promotionImageService;

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
     * 分页查询宣传图片（管理端）
     */
    @GetMapping("/page")
    public Result<Page<PromotionImage>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        checkAdmin();
        Page<PromotionImage> page = promotionImageService.page(current, size);
        return Result.ok(page);
    }

    /**
     * 查询所有启用的宣传图片（用户端，无需管理员权限）
     */
    @GetMapping("/active")
    public Result<List<PromotionImage>> listActive() {
        List<PromotionImage> list = promotionImageService.listActive();
        return Result.ok(list);
    }

    /**
     * 添加宣传图片
     */
    @PostMapping
    public Result<?> add(@RequestBody PromotionImage image) {
        checkAdmin();
        promotionImageService.add(image);
        return Result.ok("添加成功");
    }

    /**
     * 切换宣传图片启用/禁用状态
     */
    @PutMapping("/toggle/{id}")
    public Result<?> toggleActive(@PathVariable Long id) {
        checkAdmin();
        promotionImageService.toggleActive(id);
        return Result.ok();
    }

    /**
     * 软删除宣传图片（管理员端也不可见）
     */
    @DeleteMapping("/{id}")
    public Result<?> softDelete(@PathVariable Long id) {
        checkAdmin();
        promotionImageService.softDelete(id);
        return Result.ok("删除成功");
    }
}
