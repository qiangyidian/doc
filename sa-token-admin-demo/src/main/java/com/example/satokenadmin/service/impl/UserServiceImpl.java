package com.example.satokenadmin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satokenadmin.dto.CreateUserRequest;
import com.example.satokenadmin.entity.SysUser;
import com.example.satokenadmin.mapper.SysUserMapper;
import com.example.satokenadmin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    @Override
    public SysUser currentUser() {
        Long loginId = StpUtil.getLoginIdAsLong();
        return sysUserMapper.selectById(loginId);
    }

    @Override
    public List<SysUser> userList() {
        return sysUserMapper.selectAll();
    }

    @Override
    public SysUser createUser(CreateUserRequest request) {
        SysUser existUser = sysUserMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setStatus(1);
        sysUserMapper.insert(user);
        return user;
    }

    @Override
    public void kickout(Long userId) {
        // 踢人下线：让指定用户的登录态失效。
        StpUtil.kickout(userId);
    }

    @Override
    public void disable(Long userId, long disableSeconds) {
        // 封禁账号：指定时间内无法登录。
        StpUtil.disable(userId, disableSeconds);
    }

    @Override
    public Map<String, Object> userTokenState(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("loginId", userId);
        data.put("isDisable", StpUtil.isDisable(userId));
        data.put("disableSeconds", StpUtil.getDisableTime(userId));
        data.put("tokenValueList", StpUtil.getTokenValueListByLoginId(userId));
        return data;
    }
}