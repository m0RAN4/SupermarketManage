package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.service.ProfitStatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profit")
@RequiredArgsConstructor
public class ProfitStatController {

    private final ProfitStatService profitStatService;

    @Autowired
    private HttpServletRequest request;

    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    @GetMapping("/monthly")
    public Result monthly(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        checkAdmin();
        return Result.ok(profitStatService.getMonthlyProfit(year, month));
    }
}
