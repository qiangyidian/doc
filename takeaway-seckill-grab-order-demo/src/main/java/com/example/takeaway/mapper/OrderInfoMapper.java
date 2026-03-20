package com.example.takeaway.mapper;

import com.example.takeaway.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;

public interface OrderInfoMapper {

    int insert(OrderInfo orderInfo);

    OrderInfo selectByOrderNo(@Param("orderNo") String orderNo);
}