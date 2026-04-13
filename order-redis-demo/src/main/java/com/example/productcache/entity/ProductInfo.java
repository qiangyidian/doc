package com.example.productcache.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_product_info")
public class ProductInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal price;

    private Integer stockCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}