package com.example.logintoken.service;

import com.example.logintoken.dto.LoginRequest;
import com.example.logintoken.dto.LoginResponse;
import com.example.logintoken.dto.LoginUserInfo;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginUserInfo getCurrentUser(String token);

    void logout(String token);
}