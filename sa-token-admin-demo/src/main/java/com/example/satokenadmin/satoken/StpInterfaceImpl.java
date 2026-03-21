package com.example.satokenadmin.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.example.satokenadmin.mapper.SysPermissionMapper;
import com.example.satokenadmin.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // Sa-Token 在做权限校验时，会自动调用这个方法。
        // loginId 就是当前登录用户的 ID。
        return sysPermissionMapper.selectPermissionCodeListByUserId(Long.valueOf(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // Sa-Token 在做角色校验时，会自动调用这个方法。
        return sysRoleMapper.selectRoleCodeListByUserId(Long.valueOf(loginId.toString()));
    }
}