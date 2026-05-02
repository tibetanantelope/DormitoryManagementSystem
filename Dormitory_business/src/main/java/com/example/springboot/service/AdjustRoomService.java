package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.AdjustRoom;

public interface AdjustRoomService extends IService<AdjustRoom> {

    //查询调宿申请
    Page find(Integer pageNum, Integer pageSize, String search);

    //删除调宿申请
    int deleteAdjustment(Integer id);

    //更新
    int updateApply(AdjustRoom adjustRoom);

    // 添加
    int addApply(AdjustRoom adjustRoom);

    //宿管审核调宿申请
    int reviewApply(Integer id, String state);

    //个人调宿申请查询（学生用）
    Page individualFind(Integer pageNum, Integer pageSize, String search, String username);

    //管理员查询审核通过的调宿申请
    Page findApproved(Integer pageNum, Integer pageSize, String search);

    //宿管查询管辖范围内的调宿申请
    Page findForDormManager(Integer pageNum, Integer pageSize, String search, Integer dormBuildId);

}
