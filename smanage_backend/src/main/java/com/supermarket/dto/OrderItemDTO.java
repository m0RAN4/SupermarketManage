package com.supermarket.dto;
import lombok.Data;
@Data
public class OrderItemDTO {
    private Long baseProductId;
    private Long batchId;
    private Integer buyQuantity;
}
