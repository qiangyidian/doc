package com.example.productcache.service;

import com.example.productcache.entity.ProductInfo;

import java.math.BigDecimal;

public interface ProductService {

    ProductInfo getProductById(Long id);

    ProductInfo updatePrice(Long id, BigDecimal price);

    String getRawCacheValue(Long id);
}