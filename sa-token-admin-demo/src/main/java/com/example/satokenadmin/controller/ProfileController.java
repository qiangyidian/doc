package com.example.satokenadmin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.satokenadmin.common.Result;
import com.example.satokenadmin.entity.SysUser;
import com.example.satokenadmin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    @SaCheckLogin
    public Result<SysUser> profile() {
        return Result.success(userService.currentUser());
    }
}