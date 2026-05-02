package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Repair;
import com.example.springboot.service.RepairService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/repair")
public class RepairController {

    @Resource
    private RepairService repairService;

    /**
     * 添加订单
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody Repair repair) {
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
                              @RequestParam(required = false) Integer dormBuildId) {
        Page page;
        if (dormBuildId != null) {
            // 宿管查询管辖范围内的订单
            page = repairService.findForDormManager(pageNum, pageSize, search, dormBuildId);
        } else {
            // 没传楼栋ID则查询所有
            page = repairService.find(pageNum, pageSize, search);
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
    public Result<?> reviewOrder(@PathVariable Integer id, @RequestParam String state) {
        int i = repairService.reviewOrder(id, state);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "审核失败");
        }
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
}
