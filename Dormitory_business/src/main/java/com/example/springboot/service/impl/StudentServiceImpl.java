package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.PasswordUtils;
import com.example.springboot.entity.Student;
import com.example.springboot.mapper.StudentMapper;
import com.example.springboot.service.StudentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    /**
     * 注入DAO层对象
     */
    @Resource
    private StudentMapper studentMapper;

    /**
     * 学生登陆
     */
    @Override
    public Student stuLogin(String username, String password) {
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Student student = studentMapper.selectOne(qw);
        if (student != null && PasswordUtils.matches(password, student.getPassword())) {
            return student;
        } else {
            return null;
        }
    }

    /**
     * 学生新增
     */
    @Override
    public int addNewStudent(Student student) {
        student.setPassword(PasswordUtils.encode(student.getPassword()));
        int insert = studentMapper.insert(student);
        return insert;
    }

    /**
     * 分页查询学生
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.like("name", search);
        qw.orderByAsc("CAST(SUBSTRING(username, 4) AS UNSIGNED)", "username");
        Page studentPage = studentMapper.selectPage(page, qw);
        return studentPage;
    }

    /**
     * 按宿舍楼分页查询学生（宿管只能看管辖楼栋）
     */
    @Override
    public Page findByDormBuildId(Integer pageNum, Integer pageSize, String search, Integer dormBuildId) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.inSql("username",
                "SELECT b.username FROM dorm_bed b "
                        + "JOIN dorm_room r ON b.dormroom_id = r.dormroom_id "
                        + "WHERE r.dormbuild_id = " + dormBuildId
                        + " AND b.username IS NOT NULL");
        if (search != null && !search.trim().isEmpty()) {
            qw.like("name", search.trim());
        }
        qw.orderByAsc("CAST(SUBSTRING(username, 4) AS UNSIGNED)", "username");
        return studentMapper.selectPage(page, qw);
    }

    /**
     * 更新学生信息
     */
    @Override
    public int updateNewStudent(Student student) {
        handlePasswordBeforeUpdate(student);
        int i = studentMapper.updateById(student);
        return i;
    }

    private void handlePasswordBeforeUpdate(Student student) {
        if (student.getPassword() == null || student.getPassword().isEmpty()) {
            Student oldStudent = studentMapper.selectById(student.getUsername());
            if (oldStudent != null) {
                student.setPassword(oldStudent.getPassword());
            }
            return;
        }
        student.setPassword(PasswordUtils.encode(student.getPassword()));
    }

    /**
     * 删除学生信息
     */
    @Override
    public int deleteStudent(String username) {
        int i = studentMapper.deleteById(username);
        return i;
    }


    /**
     * 主页顶部：学生统计
     */
    @Override
    public int stuNum() {
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.isNotNull("username");
        int stuNum = Math.toIntExact(studentMapper.selectCount(qw));
        return stuNum;
    }

    /**
     * 床位信息，查询该学生信息
     */
    @Override
    public Student stuInfo(String username) {
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Student student = studentMapper.selectOne(qw);
        return student;
    }
}
