package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.PasswordUtils;
import com.example.springboot.entity.Admin;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.service.AdminService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    /**
     * 注入DAO层对象
     */
    @Resource
    private AdminMapper adminMapper;

    /**
     * 管理员登录
     */
    @Override
    public Admin adminLogin(String username, String password) {
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Admin admin = adminMapper.selectOne(qw);
        if (admin != null && PasswordUtils.matches(password, admin.getPassword())) {
            return admin;
        } else {
            return null;
        }
    }

    /**
     * 管理员信息更新
     */
    @Override
    public int updateAdmin(Admin admin) {
        handlePasswordBeforeUpdate(admin);
        int i = adminMapper.updateById(admin);
        return i;
    }

    private void handlePasswordBeforeUpdate(Admin admin) {
        if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
            Admin oldAdmin = adminMapper.selectById(admin.getUsername());
            if (oldAdmin != null) {
                admin.setPassword(oldAdmin.getPassword());
            }
            return;
        }
        admin.setPassword(PasswordUtils.encode(admin.getPassword()));
    }

}
