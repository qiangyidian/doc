package com.example.satokenadmin.service;

import com.example.satokenadmin.dto.CreateUserRequest;
import com.example.satokenadmin.entity.SysUser;

import java.util.List;
import java.util.Map;

public interface UserService {

    SysUser currentUser();

    List<SysUser> userList();

    SysUser createUser(CreateUserRequest request);

    void kickout(Long userId);

    void disable(Long userId, long disableSeconds);

    Map<String, Object> userTokenState(Long userId);
}