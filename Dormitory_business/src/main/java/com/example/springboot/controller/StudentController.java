package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.entity.DormManager;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.entity.Student;
import com.example.springboot.entity.User;
import com.example.springboot.service.DormBuildService;
import com.example.springboot.service.DormRoomService;
import com.example.springboot.service.StudentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/stu")
public class StudentController {

    @Resource
    private StudentService studentService;

    @Resource
    private DormRoomService dormRoomService;

    @Resource
    private DormBuildService dormBuildService;

    /**
     * 添加学生信息
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody Student student, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        int i = studentService.addNewStudent(student);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "添加失败");
        }

    }

    /**
     * 更新学生信息
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody Student student, HttpSession session) {
        if (!canManageStudent(session, student.getUsername())) {
            return Result.error("-1", "无权限操作");
        }
        if (!studentGenderMatchesCurrentRoom(student)) {
            return Result.error("-1", "学生性别与当前宿舍楼类型不匹配");
        }
        int i = studentService.updateNewStudent(student);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "更新失败");
        }
    }

    /**
     * 删除学生信息
     */
    @DeleteMapping("/delete/{username}")
    public Result<?> delete(@PathVariable String username, HttpSession session) {
        if (!canManageStudent(session, username)) {
            return Result.error("-1", "无权限操作");
        }
        int i = studentService.deleteStudent(username);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }

    /**
     * 查找学生信息
     */
    @GetMapping("/find")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              HttpSession session) {
        Page page;
        if ("dormManager".equals(AuthContext.getIdentity(session))) {
            Integer dormBuildId = getDormManagerBuildId(session);
            if (dormBuildId == null) {
                return Result.error("-1", "无权限操作");
            }
            page = studentService.findByDormBuildId(pageNum, pageSize, search, dormBuildId);
        } else {
            page = studentService.find(pageNum, pageSize, search);
        }
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 学生登录
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user, HttpSession session) {
        System.out.println(user.getUsername());
        System.out.println(user.getPassword());
        Object o = studentService.stuLogin(user.getUsername(), user.getPassword());
        if (o != null) {
            System.out.println(o);
            //存入session
            session.setAttribute("Identity", "stu");
            session.setAttribute("User", o);
            return Result.success(o);
        } else {
            return Result.error("-1", "用户名或密码错误");
        }
    }

    /**
     * 主页顶部：学生统计
     */
    @GetMapping("/stuNum")
    public Result<?> stuNum() {
        int num = studentService.stuNum();
        if (num > 0) {
            return Result.success(num);
        } else {
            return Result.error("-1", "查询失败");
        }
    }


    /**
     * 床位信息，查询是否存在该学生
     * 床位信息，查询床位上的学生信息
     */
    @GetMapping("/exist/{value}")
    public Result<?> exist(@PathVariable String value) {
        Student student = studentService.stuInfo(value);
        if (student != null) {
            return Result.success(student);
        } else {
            return Result.error("-1", "不存在该学生");
        }
    }

    private boolean isAdmin(HttpSession session) {
        return AuthContext.isAdmin(session);
    }

    private Integer getDormManagerBuildId(HttpSession session) {
        Integer dormBuildId = AuthContext.getDormBuildId(session);
        if (dormBuildId != null) {
            return dormBuildId;
        }
        Object user = session.getAttribute("User");
        if (user instanceof DormManager) {
            return ((DormManager) user).getDormBuildId();
        }
        return null;
    }

    private boolean canManageStudent(HttpSession session, String username) {
        if (isAdmin(session)) {
            return true;
        }
        if (!"dormManager".equals(AuthContext.getIdentity(session))) {
            return false;
        }
        Integer dormBuildId = getDormManagerBuildId(session);
        if (dormBuildId == null) {
            return false;
        }
        DormRoom dormRoom = dormRoomService.judgeHadBed(username);
        return dormRoom != null && dormRoom.getDormBuildId() == dormBuildId;
    }

    private boolean studentGenderMatchesCurrentRoom(Student student) {
        DormRoom dormRoom = dormRoomService.judgeHadBed(student.getUsername());
        if (dormRoom == null) {
            return true;
        }
        String expectedGender = getExpectedGender(dormRoom.getDormBuildId());
        return expectedGender == null || expectedGender.equals(student.getGender());
    }

    private String getExpectedGender(int dormBuildId) {
        QueryWrapper<DormBuild> qw = new QueryWrapper<>();
        qw.eq("dormbuild_id", dormBuildId);
        DormBuild dormBuild = dormBuildService.getOne(qw);
        if (dormBuild == null || dormBuild.getDormBuildDetail() == null) {
            return null;
        }
        if (dormBuild.getDormBuildDetail().contains("男")) {
            return "男";
        }
        if (dormBuild.getDormBuildDetail().contains("女")) {
            return "女";
        }
        return null;
    }
}
