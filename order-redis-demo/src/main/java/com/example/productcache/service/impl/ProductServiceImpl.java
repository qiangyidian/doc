package com.example.productcache.service.impl;

import com.example.productcache.common.Constants;
import com.example.productcache.entity.ProductInfo;
import com.example.productcache.mapper.ProductInfoMapper;
import com.example.productcache.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductInfoMapper productInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductServiceImpl(ProductInfoMapper productInfoMapper,
                              StringRedisTemplate stringRedisTemplate,
                              ObjectMapper objectMapper) {
        this.productInfoMapper = productInfoMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductInfo getProductById(Long id) {
        String cacheKey = Constants.PRODUCT_CACHE_KEY_PREFIX + id;

        // 第一步：先从 Redis 里查缓存。
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            try {
                return objectMapper.readValue(cacheValue, ProductInfo.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("缓存反序列化失败", e);
            }
        }

        // 第二步：Redis 没有时，再查 MySQL。
        ProductInfo productInfo = productInfoMapper.selectById(id);
        if (productInfo == null) {
            return null;
        }

        // 第三步：把 MySQL 查询结果回填到 Redis，并设置 TTL。
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(productInfo),
                    30,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("缓存序列化失败", e);
        }

        return productInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductInfo updatePrice(Long id, BigDecimal price) {
        ProductInfo productInfo = productInfoMapper.selectById(id);
        if (productInfo == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        productInfo.setPrice(price);
        productInfo.setUpdateTime(LocalDateTime.now());
        productInfoMapper.updateById(productInfo);

        // 更新数据库后，主动删除旧缓存。
        // 下次查询时，会重新从 MySQL 加载最新数据并回填到 Redis。
        stringRedisTemplate.delete(Constants.PRODUCT_CACHE_KEY_PREFIX + id);
        return productInfo;
    }

    @Override
    public String getRawCacheValue(Long id) {
        return stringRedisTemplate.opsForValue().get(Constants.PRODUCT_CACHE_KEY_PREFIX + id);
    }
}