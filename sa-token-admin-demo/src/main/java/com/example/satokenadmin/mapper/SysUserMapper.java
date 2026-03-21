package com.example.satokenadmin.mapper;

import com.example.satokenadmin.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {

    SysUser selectByUsername(@Param("username") String username);

    SysUser selectById(@Param("id") Long id);

    List<SysUser> selectAll();

    int insert(SysUser user);
}