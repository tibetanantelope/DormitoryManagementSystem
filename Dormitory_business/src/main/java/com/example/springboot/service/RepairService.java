package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.Repair;


public interface RepairService extends IService<Repair> {

    //显示订单数量
    public int showOrderNum();

    //新增订单
    int addNewOrder(Repair repair);

    //查询
    Page find(Integer pageNum, Integer pageSize, String search);

    //查询
    Page individualFind(Integer pageNum, Integer pageSize, String search, String name);

    //更新订单信息
    int updateNewOrder(Repair repair);

    //删除订单
    int deleteOrder(Integer id);

    //宿管审核报修订单
    int reviewOrder(Integer id, String state);

    //管理员查询审核通过的订单
    Page findApproved(Integer pageNum, Integer pageSize, String search);

    //宿管查询管辖范围内的报修订单
    Page findForDormManager(Integer pageNum, Integer pageSize, String search, Integer dormBuildId);
}
