package com.example.productcache.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.productcache.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductInfoMapper extends BaseMapper<ProductInfo> {
}