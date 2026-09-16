package com.supermarket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.entity.ShelfInfo;
import com.supermarket.mapper.ShelfInfoMapper;
import com.supermarket.service.ShelfInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelf")
@RequiredArgsConstructor
public class ShelfInfoController {

    private final ShelfInfoService shelfInfoService;
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
     * 分页查询货架列表（支持按编号/区域独立搜索）
     */
    @GetMapping("/page")
    public Result<Page<ShelfInfo>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String shelfCode,
            @RequestParam(required = false) String shelfArea) {
        checkAdmin();
        Page<ShelfInfo> page = shelfInfoService.page(current, size, shelfCode, shelfArea);
        return Result.ok(page);
    }

    /**
     * 根据ID查询货架
     */
    @GetMapping("/{id}")
    public Result<ShelfInfo> getById(@PathVariable Long id) {
        checkAdmin();
        ShelfInfo shelf = shelfInfoService.getById(id);
        return Result.ok(shelf);
    }

    /**
     * 查询所有已启用的货架列表
     */
    @GetMapping("/list")
    public Result<List<ShelfInfo>> list() {
        checkAdmin();
        List<ShelfInfo> list = shelfInfoMapper.selectList(
                new LambdaQueryWrapper<ShelfInfo>().eq(ShelfInfo::getIsEnabled, 1)
        );
        return Result.ok(list);
    }

    /**
     * 添加货架
     */
    @PostMapping
    public Result<?> add(@RequestBody ShelfInfo shelfInfo) {
        checkAdmin();
        shelfInfoService.add(shelfInfo);
        return Result.ok("添加成功");
    }

    /**
     * 更新货架
     */
    @PutMapping
    public Result<?> update(@RequestBody ShelfInfo shelfInfo) {
        checkAdmin();
        shelfInfoService.update(shelfInfo);
        return Result.ok("更新成功");
    }

    /**
     * 切换货架启用/禁用状态
     */
    @PutMapping("/toggle/{id}")
    public Result<?> toggleEnabled(@PathVariable Long id) {
        checkAdmin();
        shelfInfoService.toggleEnabled(id);
        return Result.ok();
    }

    /** 获取货架每层容量详情 */
    @GetMapping("/capacity/{id}")
    public Result<?> getCapacityDetails(@PathVariable Long id) {
        checkAdmin();
        return Result.ok(shelfInfoService.getCapacityDetails(id));
    }
}
