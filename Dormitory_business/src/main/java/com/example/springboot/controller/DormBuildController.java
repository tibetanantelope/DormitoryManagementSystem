package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.entity.DormManager;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.entity.Repair;
import com.example.springboot.service.DormBuildService;
import com.example.springboot.service.DormManagerService;
import com.example.springboot.service.DormRoomService;
import com.example.springboot.service.RepairService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/building")
public class DormBuildController {

    @Resource
    private DormBuildService dormBuildService;

    @Resource
    private DormManagerService dormManagerService;

    @Resource
    private DormRoomService dormRoomService;

    @Resource
    private RepairService repairService;

    /**
     * 楼宇添加
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody DormBuild dormBuild, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        int i = dormBuildService.addNewBuilding(dormBuild);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "添加失败");
        }
    }

    /**
     * 楼宇信息更新
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody DormBuild dormBuild, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        int i = dormBuildService.updateNewBuilding(dormBuild);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "更新失败");
        }
    }

    /**
     * 楼宇删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("-1", "无权限操作");
        }
        DormBuild dormBuild = dormBuildService.getById(id);
        if (dormBuild == null) {
            return Result.error("-1", "楼宇不存在");
        }
        if (isBuildReferenced(dormBuild.getDormBuildId())) {
            return Result.error("-1", "该楼宇已有宿管、房间或报修记录，不能删除");
        }
        int i = dormBuildService.deleteBuilding(id);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }

    /**
     * 楼宇查找
     */
    @GetMapping("/find")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search) {
        Page page = dormBuildService.find(pageNum, pageSize, search);
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 首页Echarts 获取楼宇信息
     */
    @GetMapping("/getBuildingName")
    public Result<?> getBuildingName() {
        List<DormBuild> buildingName = dormBuildService.getBuildingId();
        List<Integer> buildingId = buildingName.stream()
                .map(dormBuildId -> dormBuildId.getDormBuildId())
                .collect(Collectors.toList());
        return !buildingId.isEmpty() ?
                Result.success(buildingId) : Result.error("-1", "查询失败");
    }
    //        if (!buildingId.isEmpty()) {
//            return Result.success(buildingId);
//        } else {
//            return Result.error("-1", "查询失败");
//        }

    private boolean isAdmin(HttpSession session) {
        return AuthContext.isAdmin(session);
    }

    private boolean isBuildReferenced(Integer dormBuildId) {
        QueryWrapper<DormManager> managerQw = new QueryWrapper<>();
        managerQw.eq("dormbuild_id", dormBuildId);
        if (dormManagerService.count(managerQw) > 0) {
            return true;
        }
        QueryWrapper<DormRoom> roomQw = new QueryWrapper<>();
        roomQw.eq("dormbuild_id", dormBuildId);
        if (dormRoomService.count(roomQw) > 0) {
            return true;
        }
        QueryWrapper<Repair> repairQw = new QueryWrapper<>();
        repairQw.eq("dormbuild_id", dormBuildId);
        return repairService.count(repairQw) > 0;
    }
}
