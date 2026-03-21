package com.example.satokenadmin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.satokenadmin.common.Result;
import com.example.satokenadmin.dto.LoginRequest;
import com.example.satokenadmin.dto.LoginResponse;
import com.example.satokenadmin.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public Result<String> logout() {
        authService.logout();
        return Result.success("退出登录成功");
    }

    @GetMapping("/login-info")
    @SaCheckLogin
    public Result<Map<String, Object>> loginInfo() {
        return Result.success(authService.currentLoginInfo());
    }
}