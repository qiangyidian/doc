package com.example.satokenadmin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {

    private Long id;

    // 登录账号。
    private String username;

    // 登录密码。
    // 这里为了学习直接用明文，真实生产必须加密存储。
    private String password;

    // 昵称。
    private String nickname;

    // 状态，1 表示正常，0 表示业务停用。
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}