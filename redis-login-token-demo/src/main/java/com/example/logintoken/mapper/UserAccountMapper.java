package com.example.logintoken.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.logintoken.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}