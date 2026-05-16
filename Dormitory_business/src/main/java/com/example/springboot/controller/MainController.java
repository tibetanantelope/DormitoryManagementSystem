package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.AuthContext;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.DormManager;
import com.example.springboot.entity.Student;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.DormManagerService;
import com.example.springboot.service.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.annotation.Resource;

@RestController
@RequestMapping("/main")
public class MainController {

    @Resource
    private AdminService adminService;

    @Resource
    private StudentService studentService;

    @Resource
    private DormManagerService dormManagerService;

    /**
     * 获取身份信息
     */
    @GetMapping("/loadIdentity")
    public Result<?> loadIdentity(HttpSession session) {
        Object identity = AuthContext.getIdentity(session);

        if (identity != null) {
            return Result.success(identity);
        } else {
            return Result.error("-1", "加载失败");
        }
    }

    /**
     * 获取个人信息
     */
    @GetMapping("/loadUserInfo")
    public Result<?> loadUserInfo(HttpSession session) {
        String identity = AuthContext.getIdentity(session);
        String username = AuthContext.getUsername();
        Object user = findUser(identity, username);
        if (user != null) {
            clearPassword(user);
            return Result.success(user);
        } else {
            return Result.error("-1", "加载失败");
        }
    }

    /**
     * 退出登录
     */
    @GetMapping("/signOut")
    public Result<?> signOut(HttpSession session) {
        return Result.success();
    }

    private void clearPassword(Object user) {
        if (user instanceof Admin) {
            ((Admin) user).setPassword(null);
        } else if (user instanceof Student) {
            ((Student) user).setPassword(null);
        } else if (user instanceof DormManager) {
            ((DormManager) user).setPassword(null);
        }
    }

    private Object findUser(String identity, String username) {
        if (identity == null || username == null) {
            return null;
        }
        if ("admin".equals(identity)) {
            return adminService.getById(username);
        }
        if ("stu".equals(identity)) {
            return studentService.getById(username);
        }
        if ("dormManager".equals(identity)) {
            return dormManagerService.getById(username);
        }
        return null;
    }
}
