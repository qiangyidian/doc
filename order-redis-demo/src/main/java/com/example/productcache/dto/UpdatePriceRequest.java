package com.example.productcache.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePriceRequest {

    private BigDecimal price;
}