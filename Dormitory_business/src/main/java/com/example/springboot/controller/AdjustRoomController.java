package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.common.Result;
import com.example.springboot.entity.AdjustRoom;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.entity.Student;
import com.example.springboot.service.AdjustRoomService;
import com.example.springboot.service.DormBuildService;
import com.example.springboot.service.DormRoomService;
import com.example.springboot.service.StudentService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/adjustRoom")
public class AdjustRoomController {

    @Resource
    private AdjustRoomService adjustRoomService;

    @Resource
    private DormRoomService dormRoomService;

    @Resource
    private StudentService studentService;

    @Resource
    private DormBuildService dormBuildService;


    /**
     * 添加调宿申请
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody AdjustRoom adjustRoom) {
        try {
            // 如果状态为空，设置默认值
            if (adjustRoom.getState() == null || adjustRoom.getState().isEmpty()) {
                adjustRoom.setState("未处理");
            }
            String genderError = validateTargetRoomGender(adjustRoom);
            if (genderError != null) {
                return Result.error("-1", genderError);
            }
            int result = adjustRoomService.addApply(adjustRoom);
            if (result == 1) {
                return Result.success();
            } else {
                return Result.error("-1", "添加失败");
            }
        } catch (Exception e) {
            return Result.error("-1", "添加失败: " + e.getMessage());
        }
    }


    /**
     * 更新订单（管理员执行调宿操作）
     */
    @PutMapping("/update/{state}")
    @Transactional
    public Result<?> update(@RequestBody AdjustRoom adjustRoom, @PathVariable Boolean state) {
        if (state) {
            AdjustRoom oldApply = adjustRoomService.getById(adjustRoom.getId());
            if (oldApply == null) {
                return Result.error("-1", "调宿申请不存在");
            }
            if (!"通过".equals(oldApply.getState()) && !"approved".equals(oldApply.getState())) {
                return Result.error("-1", "只有审核通过的申请才能执行调宿");
            }
            String genderError = validateTargetRoomGender(adjustRoom);
            if (genderError != null) {
                return Result.error("-1", genderError);
            }
            Map<String, Object> result = adjustRoomService.executeAdjustRoom(adjustRoom.getId());
            int resultCode = ((Number) result.get("resultCode")).intValue();
            if (resultCode != 1) {
                return Result.error("-1", String.valueOf(result.get("resultMsg")));
            }
            return Result.success();
        }
        //更新调宿表信息
        int i = adjustRoomService.updateApply(adjustRoom);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "更新失败");
        }
    }

    /**
     * 宿管审核调宿申请
     */
    @PutMapping("/review/{id}")
    public Result<?> reviewApply(@PathVariable Integer id, @RequestParam String state) {
        try {
            System.out.println("=== 审核调宿申请 ===");
            System.out.println("id: " + id);
            System.out.println("state: " + state);
            int i = adjustRoomService.reviewApply(id, state);
            if (i == 1) {
                System.out.println("审核成功");
                return Result.success();
            } else {
                System.out.println("审核失败：更新返回 " + i);
                return Result.error("-1", "审核失败");
            }
        } catch (Exception e) {
            System.out.println("审核异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("-1", "审核失败: " + e.getMessage());
        }
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        int i = adjustRoomService.deleteAdjustment(id);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }

    /**
     * 查找订单（宿管/管理员用）
     * 注意：此接口必须放在 /find/{username} 之前，与报修申请保持一致
     */
    @GetMapping("/find")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(required = false) Integer dormBuildId) {
        Page page;
        if (dormBuildId != null) {
            // 宿管查询管辖范围内的申请
            page = adjustRoomService.findForDormManager(pageNum, pageSize, search, dormBuildId);
        } else {
            // 管理员查询所有
            page = adjustRoomService.find(pageNum, pageSize, search);
        }
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 个人调宿申请查询（学生用）
     * 注意：此接口必须放在 /find 之后，与报修申请保持一致
     */
    @GetMapping("/find/{username}")
    public Result<?> individualFind(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(defaultValue = "") String search,
                                    @PathVariable String username) {
        System.out.println("=== 调宿申请查询接口被调用 ===");
        System.out.println("username: " + username);
        System.out.println("pageNum: " + pageNum);
        System.out.println("pageSize: " + pageSize);
        System.out.println("search: " + search);
        try {
            Page page = adjustRoomService.individualFind(pageNum, pageSize, search, username);
            if (page != null) {
                System.out.println("查询成功，返回 " + page.getTotal() + " 条记录");
                return Result.success(page);
            } else {
                System.out.println("查询失败：page为null");
                return Result.error("-1", "查询失败");
            }
        } catch (Exception e) {
            System.out.println("查询异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("-1", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 管理员查询审核通过的调宿申请
     */
    @GetMapping("/findApproved")
    public Result<?> findApproved(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(defaultValue = "") String search) {
        Page page = adjustRoomService.findApproved(pageNum, pageSize, search);
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    private String validateTargetRoomGender(AdjustRoom adjustRoom) {
        Student student = studentService.stuInfo(adjustRoom.getUsername());
        DormRoom currentRoom = dormRoomService.checkRoomExist(adjustRoom.getCurrentRoomId());
        DormRoom targetRoom = dormRoomService.checkRoomExist(adjustRoom.getTowardsRoomId());
        if (student == null) {
            return "申请学生不存在";
        }
        if (currentRoom == null) {
            return "当前房间不存在";
        }
        if (targetRoom == null) {
            return "目标房间不存在";
        }
        String expectedGender = getExpectedGender(targetRoom.getDormBuildId());
        if (expectedGender != null && !expectedGender.equals(student.getGender())) {
            return "目标宿舍楼性别类型不匹配";
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
