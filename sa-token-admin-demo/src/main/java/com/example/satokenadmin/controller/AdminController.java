package com.example.satokenadmin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.satokenadmin.common.Result;
import com.example.satokenadmin.dto.CreateUserRequest;
import com.example.satokenadmin.dto.DisableUserRequest;
import com.example.satokenadmin.entity.SysUser;
import com.example.satokenadmin.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @SaCheckPermission("user.query")
    public Result<List<SysUser>> userList() {
        return Result.success(userService.userList());
    }

    @PostMapping("/users")
    @SaCheckPermission("user.add")
    public Result<SysUser> createUser(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    @PostMapping("/kickout/{userId}")
    @SaCheckRole("admin")
    @SaCheckPermission("user.kickout")
    public Result<String> kickout(@PathVariable Long userId) {
        userService.kickout(userId);
        return Result.success("已将用户踢下线");
    }

    @PostMapping("/disable/{userId}")
    @SaCheckRole("admin")
    @SaCheckPermission("user.disable")
    public Result<String> disable(@PathVariable Long userId,
                                  @Valid @RequestBody DisableUserRequest request) {
        userService.disable(userId, request.getDisableSeconds());
        return Result.success("封禁成功");
    }

    @GetMapping("/token-state/{userId}")
    @SaCheckRole("admin")
    public Result<Map<String, Object>> tokenState(@PathVariable Long userId) {
        return Result.success(userService.userTokenState(userId));
    }
}