package com.example.logintoken.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.logintoken.common.Constants;
import com.example.logintoken.dto.LoginRequest;
import com.example.logintoken.dto.LoginResponse;
import com.example.logintoken.dto.LoginUserInfo;
import com.example.logintoken.entity.UserAccount;
import com.example.logintoken.mapper.UserAccountMapper;
import com.example.logintoken.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper) {
        this.userAccountMapper = userAccountMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUsername, request.getUsername())
                        .eq(UserAccount::getPassword, request.getPassword())
                        .last("limit 1")
        );

        if (userAccount == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId(userAccount.getId());
        loginUserInfo.setUsername(userAccount.getUsername());
        loginUserInfo.setNickname(userAccount.getNickname());

        try {
            stringRedisTemplate.opsForValue().set(
                    Constants.LOGIN_TOKEN_PREFIX + token,
                    objectMapper.writeValueAsString(loginUserInfo),
                    Constants.TOKEN_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("登录用户信息序列化失败", e);
        }

        return new LoginResponse(token, Constants.TOKEN_EXPIRE_MINUTES);
    }

    @Override
    public LoginUserInfo getCurrentUser(String token) {
        String cacheValue = stringRedisTemplate.opsForValue().get(Constants.LOGIN_TOKEN_PREFIX + token);
        if (cacheValue == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cacheValue, LoginUserInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("登录用户信息反序列化失败", e);
        }
    }

    @Override
    public void logout(String token) {
        // 退出登录本质上就是删除 token 对应的 Redis key。
        stringRedisTemplate.delete(Constants.LOGIN_TOKEN_PREFIX + token);
    }
}