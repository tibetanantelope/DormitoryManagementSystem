package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.entity.Repair;
import com.example.springboot.entity.Student;
import com.example.springboot.service.DormBuildService;
import com.example.springboot.service.DormRoomService;
import com.example.springboot.service.RepairService;
import com.example.springboot.service.StudentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/repair")
public class RepairController {

    @Resource
    private RepairService repairService;

    @Resource
    private StudentService studentService;

    @Resource
    private DormBuildService dormBuildService;

    @Resource
    private DormRoomService dormRoomService;

    /**
     * 添加订单
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody Repair repair) {
        String referenceError = validateReferences(repair);
        if (referenceError != null) {
            return Result.error("-1", referenceError);
        }
        int i = repairService.addNewOrder(repair);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "添加失败");
        }
    }

    /**
     * 更新订单
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody Repair repair) {
        String referenceError = validateReferences(repair);
        if (referenceError != null) {
            return Result.error("-1", referenceError);
        }
        int i = repairService.updateNewOrder(repair);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "更新失败");
        }
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Integer id) {

        Repair repair = repairService.getById(id);
        if (repair == null) {
            return Result.error("-1", "记录不存在");
        }

        //状态限制：允许删除“已完成”或“审核未通过”
        if (!"completed".equals(repair.getState()) && !"rejected".equals(repair.getState())) {
            return Result.error("-1", "只能删除已完成或审核未通过的记录");
        }

        boolean removed = repairService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }


    /**
     * 查找订单（宿管用 - 查询管辖范围内的订单）
     */
    @GetMapping("/find")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(required = false) Integer dormBuildId,
                              HttpSession session) {
        Page page;
        if ("dormManager".equals(AuthContext.getIdentity(session))) {
            Integer managerBuildId = AuthContext.getDormBuildId(session);
            if (managerBuildId == null) {
                return Result.error("-1", "无权限操作");
            }
            page = repairService.findForDormManager(pageNum, pageSize, search, managerBuildId);
        } else if (AuthContext.isAdmin(session) && dormBuildId != null) {
            // 宿管查询管辖范围内的订单
            page = repairService.findForDormManager(pageNum, pageSize, search, dormBuildId);
        } else if (AuthContext.isAdmin(session)) {
            // 没传楼栋ID则查询所有
            page = repairService.find(pageNum, pageSize, search);
        } else {
            return Result.error("-1", "无权限操作");
        }
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 管理员查询审核通过的订单
     */
    @GetMapping("/findApproved")
    public Result<?> findApproved(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(defaultValue = "") String search) {
        Page page = repairService.findApproved(pageNum, pageSize, search);
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 个人申报报修 分页查询（学生用）
     */
    @GetMapping("/find/{name}")
    public Result<?> individualFind(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(defaultValue = "") String search,
                                    @PathVariable String name) {
        System.out.println(name);
        Page page = repairService.individualFind(pageNum, pageSize, search, name);
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 宿管审核报修订单
     */
    @PutMapping("/review/{id}")
    public Result<?> reviewOrder(@PathVariable Integer id, @RequestParam String state, HttpSession session) {
        if (!canReviewRepair(id, session)) {
            return Result.error("-1", "无权限操作");
        }
        int i = repairService.reviewOrder(id, state);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "审核失败");
        }
    }

    private boolean canReviewRepair(Integer repairId, HttpSession session) {
        if (AuthContext.isAdmin(session)) {
            return true;
        }
        if (!"dormManager".equals(AuthContext.getIdentity(session))) {
            return false;
        }
        Integer dormBuildId = AuthContext.getDormBuildId(session);
        if (dormBuildId == null) {
            return false;
        }
        Repair repair = repairService.getById(repairId);
        return repair != null && repair.getDormBuildId() != null && repair.getDormBuildId().intValue() == dormBuildId;
    }

    /**
     * 首页顶部：报修统计
     */
    @GetMapping("/orderNum")
    public Result<?> orderNum() {
        int num = repairService.showOrderNum();
        if (num >= 0) {
            return Result.success(num);
        } else {
            return Result.error("-1", "报修统计查询失败");
        }
    }

    private String validateReferences(Repair repair) {
        Student student = studentService.getById(repair.getRepairer());
        if (student == null) {
            return "报修学生不存在";
        }
        if (!buildingExists(repair.getDormBuildId())) {
            return "报修楼栋不存在";
        }
        DormRoom dormRoom = repair.getDormRoomId() == null ? null : dormRoomService.getById(repair.getDormRoomId().intValue());
        if (dormRoom == null) {
            return "报修房间不存在";
        }
        if (repair.getDormBuildId() != null && dormRoom.getDormBuildId() != repair.getDormBuildId().intValue()) {
            return "报修房间不属于所选楼栋";
        }
        return null;
    }

    private boolean buildingExists(Long dormBuildId) {
        if (dormBuildId == null) {
            return false;
        }
        QueryWrapper<DormBuild> qw = new QueryWrapper<>();
        qw.eq("dormbuild_id", dormBuildId);
        return dormBuildService.count(qw) > 0;
    }
}
