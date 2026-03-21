package com.example.satokenadmin.entity;

import lombok.Data;

@Data
public class SysRole {

    private Long id;

    // 角色标识，例如 admin、user。
    private String roleCode;

    // 角色名称。
    private String roleName;
}