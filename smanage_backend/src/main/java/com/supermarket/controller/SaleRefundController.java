package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.dto.RefundDTO;
import com.supermarket.service.SaleRefundService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
public class SaleRefundController {

    private final SaleRefundService saleRefundService;

    @Autowired
    private HttpServletRequest request;

    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /** 分页查询退货记录 */
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer handleStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok(saleRefundService.page(pageNum, pageSize, orderId, handleStatus, startTime, endTime));
    }

    /** 用户提交退货申请 */
    @PostMapping
    public Result<?> refund(@RequestBody RefundDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        dto.setOperatorId(userId);
        saleRefundService.refund(dto);
        return Result.ok("退货申请已提交，等待管理员审核");
    }

    /** 管理员同意退货 */
    @PostMapping("/approve")
    public Result<?> approve(@RequestBody Map<String, Object> body) {
        checkAdmin();
        Long refundId = Long.valueOf(body.get("refundId").toString());
        Integer backStockQuantity = body.get("backStockQuantity") != null
                ? Integer.valueOf(body.get("backStockQuantity").toString()) : 0;
        Integer toLossQuantity = body.get("toLossQuantity") != null
                ? Integer.valueOf(body.get("toLossQuantity").toString()) : 0;
        saleRefundService.approveRefund(refundId, backStockQuantity, toLossQuantity);
        return Result.ok("退货已同意");
    }

    /** 管理员拒绝退货 */
    @PostMapping("/reject")
    public Result<?> reject(@RequestBody Map<String, Object> body) {
        checkAdmin();
        Long refundId = Long.valueOf(body.get("refundId").toString());
        saleRefundService.rejectRefund(refundId);
        return Result.ok("退货已拒绝");
    }

    /** 批量同意退货 */
    @PostMapping("/batch-approve")
    public Result<?> batchApprove(@RequestBody Map<String, Object> body) {
        checkAdmin();
        @SuppressWarnings("unchecked")
        List<Long> refundIds = (List<Long>) body.get("refundIds");
        @SuppressWarnings("unchecked")
        List<Integer> backQtys = (List<Integer>) body.get("backStockQuantities");
        @SuppressWarnings("unchecked")
        List<Integer> lossQtys = (List<Integer>) body.get("toLossQuantities");
        saleRefundService.batchApprove(refundIds, backQtys, lossQtys);
        return Result.ok("批量同意成功");
    }

    /** 批量拒绝退货 */
    @PostMapping("/batch-reject")
    public Result<?> batchReject(@RequestBody Map<String, Object> body) {
        checkAdmin();
        @SuppressWarnings("unchecked")
        List<Long> refundIds = (List<Long>) body.get("refundIds");
        saleRefundService.batchReject(refundIds);
        return Result.ok("批量拒绝成功");
    }
}
