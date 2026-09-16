package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.service.PurchaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Autowired
    private HttpServletRequest request;

    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /** 分页查询进货记录 */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long baseProductId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        checkAdmin();
        return Result.ok(purchaseService.page(current, size, baseProductId, startDate, endDate));
    }

    /** 新增进货（批次明细模式） */
    @PostMapping
    public Result<?> add(@RequestBody Map<String, Object> purchaseData) {
        checkAdmin();
        purchaseService.add(purchaseData);
        return Result.ok("进货入库成功");
    }
}
