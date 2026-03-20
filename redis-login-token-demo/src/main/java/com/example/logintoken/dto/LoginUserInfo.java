package com.example.logintoken.dto;

import lombok.Data;

/**
 * 这个对象表示“当前登录用户”写入 Redis 后的结构。
 */
@Data

public class LoginUserInfo {

    private Long userId;

    private String username;

    private String nickname;
}