package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.dto.OrderItemDTO;
import com.supermarket.entity.SaleOrder;
import com.supermarket.service.SaleOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class SaleOrderController {

    private final SaleOrderService saleOrderService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer payStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = (Integer) request.getAttribute("userType");
        Long filterUserId = (userType != null && userType == 1) ? userId : null;
        return Result.ok(saleOrderService.page(pageNum, pageSize, payStatus, startTime, endTime, filterUserId));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        Integer userType = (Integer) request.getAttribute("userType");
        Long userId = (Long) request.getAttribute("userId");

        SaleOrder order = saleOrderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (userType != null && userType == 1 && !order.getUserId().equals(userId)) {
            return Result.error(403, "无权限访问");
        }

        // 返回含批次信息的订单详情
        return Result.ok(saleOrderService.getOrderDetail(id));
    }

    @PostMapping("/create")
    public Result<?> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) body.get("items");
        if (itemsList == null || itemsList.isEmpty()) {
            throw new BusinessException("订单商品不能为空");
        }
        List<OrderItemDTO> items = new ArrayList<>();
        for (Map<String, Object> item : itemsList) {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setBaseProductId(Long.valueOf(item.get("baseProductId").toString()));
            dto.setBuyQuantity(Integer.valueOf(item.get("buyQuantity").toString()));
            if (item.get("batchId") != null) {
                dto.setBatchId(Long.valueOf(item.get("batchId").toString()));
            }
            items.add(dto);
        }
        Long orderId = saleOrderService.createOrder(userId, items);
        SaleOrder order = saleOrderService.getById(orderId);
        return Result.ok(Map.of("orderId", orderId, "payAmount", order.getPayAmount()));
    }

    @PostMapping("/pay")
    public Result<?> payOrder(@RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = Long.valueOf(body.get("orderId").toString());
        saleOrderService.payOrder(orderId, userId);
        return Result.ok("支付成功");
    }

    @PostMapping("/cancel")
    public Result<?> cancelOrder(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        saleOrderService.cancelOrder(orderId);
        return Result.ok("订单已取消");
    }

    /** 管理员编辑订单支付状态 */
    @PutMapping("/pay-status")
    public Result<?> updatePayStatus(@RequestBody Map<String, Object> body) {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Integer payStatus = Integer.valueOf(body.get("payStatus").toString());
        saleOrderService.updatePayStatus(orderId, payStatus);
        return Result.ok("订单状态已更新");
    }
}
