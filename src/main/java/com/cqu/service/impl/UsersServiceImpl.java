package com.cqu.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.entity.Users;
import com.cqu.mapper.UsersMapper;
import com.cqu.service.IUsersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements IUsersService {

    @Override
    public Users register(Users users) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getUsername, users.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 密码加密
        users.setPassword(BCrypt.hashpw(users.getPassword()));

        // 默认角色：市政人员
        if (users.getRole() == null || users.getRole().isBlank()) {
            users.setRole("MUNICIPAL_STAFF");
        }

        this.save(users);
        return users;
    }

    @Override
    public Users login(String username, String password) {
        // 根据用户名查询用户
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getUsername, username);
        Users users = this.getOne(wrapper);

        if (users == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!BCrypt.checkpw(password, users.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        return users;
    }
}
