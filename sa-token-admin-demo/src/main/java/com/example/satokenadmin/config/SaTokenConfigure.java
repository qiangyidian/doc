package com.example.satokenadmin.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 统一拦截 /user/** 和 /admin/** 下的接口，要求必须先登录。
            SaRouter.match("/user/**", r -> StpUtil.checkLogin());
            SaRouter.match("/admin/**", r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}