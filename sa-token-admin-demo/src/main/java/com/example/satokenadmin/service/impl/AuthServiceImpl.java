package com.example.satokenadmin.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satokenadmin.dto.LoginRequest;
import com.example.satokenadmin.dto.LoginResponse;
import com.example.satokenadmin.entity.SysUser;
import com.example.satokenadmin.mapper.SysPermissionMapper;
import com.example.satokenadmin.mapper.SysRoleMapper;
import com.example.satokenadmin.mapper.SysUserMapper;
import com.example.satokenadmin.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }
        if (!Objects.equals(user.getPassword(), request.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new IllegalArgumentException("当前账号已被业务停用");
        }

        // 如果这个账号此前被 Sa-Token 封禁了，checkDisable 会直接抛异常。
        StpUtil.checkDisable(user.getId());

        // 执行登录。
        // 只需要传入登录用户的唯一标识即可，这里传用户 ID。
        StpUtil.login(user.getId());

        // 登录成功后，可以往当前会话里放一些业务数据。
        // 这些数据会跟随当前登录态一起保存到 Redis。
        SaSession session = StpUtil.getSession();
        //这里进行的session是直接将session塞入到了这个对话的session中，本次对话的过程中只要进行获取都可以进行获取到本次进行塞进去的数据
        session.set("userId", user.getId());
        session.set("username", user.getUsername());
        session.set("nickname", user.getNickname());

        List<String> roleList = sysRoleMapper.selectRoleCodeListByUserId(user.getId());
        List<String> permissionList = sysPermissionMapper.selectPermissionCodeListByUserId(user.getId());

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setTokenName(StpUtil.getTokenInfo().getTokenName());
        response.setTokenValue(StpUtil.getTokenInfo().getTokenValue());
        response.setTokenTimeout(StpUtil.getTokenInfo().getTokenTimeout());
        response.setRoleList(roleList);
        response.setPermissionList(permissionList);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", session.get("userId"));
        sessionData.put("username", session.get("username"));
        sessionData.put("nickname", session.get("nickname"));
        response.setSessionData(sessionData);
        return response;
    }

    @Override
    public void logout() {
        // 退出登录后，当前 token 对应的登录态会被清除。
        StpUtil.logout();
    }

    @Override
    public Map<String, Object> currentLoginInfo() {
        // 先检查当前请求是否已登录。
        StpUtil.checkLogin();

        Map<String, Object> data = new HashMap<>();
        data.put("loginId", StpUtil.getLoginIdAsLong());
        data.put("tokenName", StpUtil.getTokenInfo().getTokenName());
        data.put("tokenValue", StpUtil.getTokenInfo().getTokenValue());
        data.put("tokenTimeout", StpUtil.getTokenInfo().getTokenTimeout());
        data.put("isLogin", StpUtil.isLogin());
        data.put("sessionUsername", StpUtil.getSession().get("username"));
        data.put("sessionNickname", StpUtil.getSession().get("nickname"));
        return data;
    }
}