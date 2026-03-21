package com.example.satokenadmin.entity;

import lombok.Data;

@Data
public class SysPermission {

    private Long id;

    // 权限标识，例如 user.query。
    private String permissionCode;

    // 权限名称。
    private String permissionName;
}