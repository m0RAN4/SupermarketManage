package com.supermarket.dto;
import lombok.Data;
@Data
public class RefundDTO {
    private Long orderId;
    private Long orderItemId;
    private Long operatorId;
    private Integer refundQuantity;
    private String refundReason;
}
