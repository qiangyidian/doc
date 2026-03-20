package com.example.logintoken.controller;

import com.example.logintoken.common.Result;
import com.example.logintoken.dto.LoginRequest;
import com.example.logintoken.dto.LoginResponse;
import com.example.logintoken.dto.LoginUserInfo;
import com.example.logintoken.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<LoginUserInfo> currentUser(@RequestParam String token) {
        return Result.success(authService.getCurrentUser(token));
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestParam String token) {
        authService.logout(token);
        return Result.success("退出登录成功");
    }
}