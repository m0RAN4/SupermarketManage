package com.supermarket.dto;

import lombok.Data;

@Data
public class CheckItemDTO {
    private Long stockProductId;
    private Integer bookSale;
    private Integer actualSale;
    private Integer bookStock;
    private Integer actualStock;
    private String remark;
}
