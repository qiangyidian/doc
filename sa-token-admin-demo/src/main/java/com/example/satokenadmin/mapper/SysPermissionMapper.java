package com.example.satokenadmin.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysPermissionMapper {

    List<String> selectPermissionCodeListByUserId(@Param("userId") Long userId);
}