package com.example.satokenadmin.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LoginResponse {

    // 当前登录用户 ID。
    private Long userId;

    // 用户名。
    private String username;

    // 昵称。
    private String nickname;

    // token 名称，例如 satoken。
    private String tokenName;

    // token 值。
    private String tokenValue;

    // token 剩余有效期。
    private Long tokenTimeout;

    // 当前用户的角色列表。
    private List<String> roleList;

    // 当前用户的权限列表。
    private List<String> permissionList;

    // 登录会话中保存的演示数据。
    private Map<String, Object> sessionData;
}