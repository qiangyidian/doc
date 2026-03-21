package com.example.satokenadmin.service;

import com.example.satokenadmin.dto.LoginRequest;
import com.example.satokenadmin.dto.LoginResponse;

import java.util.Map;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout();

    Map<String, Object> currentLoginInfo();
}