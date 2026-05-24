package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.entity.AdjustRoom;
import com.example.springboot.entity.Repair;
import com.example.springboot.entity.Student;
import com.example.springboot.service.AdjustRoomService;
import com.example.springboot.service.DormBuildService;
import com.example.springboot.service.DormRoomService;
import com.example.springboot.service.RepairService;
import com.example.springboot.service.StudentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@RestController
@RequestMapping("/room")
public class DormRoomController {

    @Resource
    private DormRoomService dormRoomService;

    @Resource
    private DormBuildService dormBuildService;

    @Resource
    private StudentService studentService;

    @Resource
    private AdjustRoomService adjustRoomService;

    @Resource
    private RepairService repairService;

    /**
     * 添加房间
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody DormRoom dormRoom, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        if (!buildingExists(dormRoom.getDormBuildId())) {
            return Result.error("-1", "该楼栋不存在");
        }
        String genderError = validateRoomGender(dormRoom);
        if (genderError != null) {
            return Result.error("-1", genderError);
        }
        int i = dormRoomService.addNewRoom(dormRoom);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "添加失败");
        }
    }

    /**
     * 更新房间
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody DormRoom dormRoom, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        if (!buildingExists(dormRoom.getDormBuildId())) {
            return Result.error("-1", "该楼栋不存在");
        }
        String genderError = validateRoomGender(dormRoom);
        if (genderError != null) {
            return Result.error("-1", genderError);
        }
        int i = dormRoomService.updateNewRoom(dormRoom);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "更新失败");
        }
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/delete/{dormRoomId}")
    public Result<?> delete(@PathVariable Integer dormRoomId, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        DormRoom dormRoom = dormRoomService.getById(dormRoomId);
        if (dormRoom == null) {
            return Result.error("-1", "房间不存在");
        }
        if (hasAnyStudent(dormRoom)) {
            return Result.error("-1", "该房间还有学生入住，不能删除");
        }
        if (isRoomReferenced(dormRoomId)) {
            return Result.error("-1", "该房间已有调宿或报修记录，不能删除");
        }
        int i = dormRoomService.deleteRoom(dormRoomId);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }

    /**
     * 查找房间
     */
    @GetMapping("/find")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search) {
        Page page = dormRoomService.find(pageNum, pageSize, search);
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 首页顶部：空宿舍统计
     */
    @GetMapping("/noFullRoom")
    public Result<?> noFullRoom() {
        int num = dormRoomService.notFullRoom();
        if (num >= 0) {
            return Result.success(num);
        } else {
            return Result.error("-1", "空宿舍查询失败");
        }
    }

    /**
     * 删除床位学生信息
     */
    @DeleteMapping("/delete/{bedName}/{dormRoomId}/{calCurrentNum}")
    public Result<?> deleteBedInfo(@PathVariable String bedName, @PathVariable Integer dormRoomId, @PathVariable int calCurrentNum, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        int i = dormRoomService.deleteBedInfo(bedName, dormRoomId, calCurrentNum);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }

    /**
     * 床位信息，查询该学生是否已有床位
     */
    @GetMapping("/judgeHadBed/{value}")
    public Result<?> judgeHadBed(@PathVariable String value) {
        DormRoom dormRoom = dormRoomService.judgeHadBed(value);
        if (dormRoom == null) {
            return Result.success();
        } else {
            return Result.error("-1", "该学生已有宿舍");
        }
    }

    /**
     * 主页 住宿人数
     */
    @GetMapping("/selectHaveRoomStuNum")
    public Result<?> selectHaveRoomStuNum() {
        Long count = dormRoomService.selectHaveRoomStuNum();
        if (count >= 0) {
            return Result.success(count);
        } else {
            return Result.error("-1", "查询首页住宿人数失败");
        }
    }

    /**
     * 住宿分布人数
     */
    @GetMapping("/getEachBuildingStuNum/{num}")
    public Result<?> getEachBuildingStuNum(@PathVariable int num) {
        ArrayList<Long> arrayList = new ArrayList();
        for (int i = 1; i <= num; i++) {
            Long eachBuildingStuNum = dormRoomService.getEachBuildingStuNum(i);
            arrayList.add(eachBuildingStuNum);
        }

        if (!arrayList.isEmpty()) {
            return Result.success(arrayList);
        } else {
            return Result.error("-1", "获取人数失败");
        }
    }

    /**
     * 学生功能： 我的宿舍
     */
    @GetMapping("/getMyRoom/{name}")
    public Result<?> getMyRoom(@PathVariable String name) {
        DormRoom dormRoom = dormRoomService.judgeHadBed(name);
        if (dormRoom != null) {
            return Result.success(dormRoom);
        } else {
            return Result.error("-1", "不存在该生");
        }
    }

    /**
     * 检查房间是否满员
     */
    @GetMapping("/checkRoomState/{dormRoomId}")
    public Result<?> checkRoomState(@PathVariable Integer dormRoomId) {
        DormRoom dormRoom = dormRoomService.checkRoomState(dormRoomId);
        if (dormRoom != null) {
            return Result.success(dormRoom);
        } else {
            return Result.error("-1", "该房间人满了");
        }
    }

    /**
     * 检查床位是否已经有人
     */
    @GetMapping("/checkBedState/{dormRoomId}/{bedNum}")
    public Result<?> getMyRoom(@PathVariable Integer dormRoomId, @PathVariable int bedNum) {
        DormRoom dormRoom = dormRoomService.checkBedState(dormRoomId, bedNum);
        if (dormRoom != null) {
            return Result.success(dormRoom);
        } else {
            return Result.error("-1", "该床位已有人");
        }
    }

    /**
     * 检查房间是否满员
     */
    @GetMapping("/checkRoomExist/{dormRoomId}")
    public Result<?> checkRoomExist(@PathVariable Integer dormRoomId) {
        DormRoom dormRoom = dormRoomService.checkRoomExist(dormRoomId);
        if (dormRoom != null) {
            return Result.success(dormRoom);
        } else {
            return Result.error("-1", "不存在该房间");
        }
    }

    private boolean isAdmin(HttpSession session) {
        return AuthContext.isAdmin(session);
    }

    private boolean buildingExists(int dormBuildId) {
        QueryWrapper<DormBuild> qw = new QueryWrapper<>();
        qw.eq("dormbuild_id", dormBuildId);
        return dormBuildService.count(qw) > 0;
    }

    private boolean isRoomReferenced(Integer dormRoomId) {
        QueryWrapper<AdjustRoom> adjustQw = new QueryWrapper<>();
        adjustQw.eq("currentroom_id", dormRoomId).or().eq("towardsroom_id", dormRoomId);
        if (adjustRoomService.count(adjustQw) > 0) {
            return true;
        }
        QueryWrapper<Repair> repairQw = new QueryWrapper<>();
        repairQw.eq("dormroom_id", dormRoomId);
        return repairService.count(repairQw) > 0;
    }

    private boolean hasAnyStudent(DormRoom dormRoom) {
        return hasText(dormRoom.getFirstBed())
                || hasText(dormRoom.getSecondBed())
                || hasText(dormRoom.getThirdBed())
                || hasText(dormRoom.getFourthBed());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String validateRoomGender(DormRoom dormRoom) {
        String expectedGender = getExpectedGender(dormRoom.getDormBuildId());
        if (expectedGender == null) {
            return null;
        }
        String error = validateBedGender(dormRoom.getFirstBed(), expectedGender);
        if (error != null) return error;
        error = validateBedGender(dormRoom.getSecondBed(), expectedGender);
        if (error != null) return error;
        error = validateBedGender(dormRoom.getThirdBed(), expectedGender);
        if (error != null) return error;
        return validateBedGender(dormRoom.getFourthBed(), expectedGender);
    }

    private String validateBedGender(String username, String expectedGender) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        Student student = studentService.stuInfo(username);
        if (student == null) {
            return "床位学生不存在";
        }
        if (!expectedGender.equals(student.getGender())) {
            return "学生" + username + "性别与宿舍楼类型不匹配";
        }
        return null;
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
