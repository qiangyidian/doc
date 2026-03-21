package com.example.satokenadmin.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleMapper {

    List<String> selectRoleCodeListByUserId(@Param("userId") Long userId);
}