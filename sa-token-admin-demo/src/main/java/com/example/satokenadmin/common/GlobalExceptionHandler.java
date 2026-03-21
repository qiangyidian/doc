package com.example.satokenadmin.common;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<String> handleNotLogin(NotLoginException e) {
        return Result.fail("当前未登录，请先登录后再访问");
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<String> handleNotPermission(NotPermissionException e) {
        return Result.fail("没有权限，缺少权限码：" + e.getPermission());
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<String> handleNotRole(NotRoleException e) {
        return Result.fail("没有角色，缺少角色标识：" + e.getRole());
    }

    @ExceptionHandler(DisableServiceException.class)
    public Result<String> handleDisable(DisableServiceException e) {
        return Result.fail("账号已被封禁，剩余封禁时间（秒）：" + e.getDisableTime());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValid(MethodArgumentNotValidException e) {
        return Result.fail("参数校验失败：" + e.getBindingResult().getFieldError().getDefaultMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统异常：" + e.getMessage());
    }
}