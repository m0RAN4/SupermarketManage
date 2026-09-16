package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.service.LossRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loss")
@RequiredArgsConstructor
public class LossRecordController {

    private final LossRecordService lossRecordService;

    @Autowired
    private HttpServletRequest request;

    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /** 分页查询损耗记录 */
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long baseProductId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        checkAdmin();
        return Result.ok(lossRecordService.page(pageNum, pageSize, baseProductId, startTime, endTime));
    }

    /** 新增损耗（批次明细选择模式） */
    @PostMapping
    public Result<?> add(@RequestBody Map<String, Object> lossData) {
        checkAdmin();
        lossRecordService.add(lossData);
        return Result.ok("损耗登记成功");
    }

    /** 月度损耗统计 */
    @GetMapping("/monthly-stats")
    public Result<?> monthlyStats(@RequestParam Integer year, @RequestParam Integer month) {
        checkAdmin();
        return Result.ok(lossRecordService.getMonthlyStats(year, month));
    }
}
