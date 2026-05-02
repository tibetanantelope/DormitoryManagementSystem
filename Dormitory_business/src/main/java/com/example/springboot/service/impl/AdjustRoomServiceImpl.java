package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.AdjustRoom;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.mapper.AdjustRoomMapper;
import com.example.springboot.mapper.DormRoomMapper;
import com.example.springboot.service.AdjustRoomService;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdjustRoomServiceImpl extends ServiceImpl<AdjustRoomMapper, AdjustRoom> implements AdjustRoomService {


    @Resource
    private AdjustRoomMapper adjustRoomMapper;

    /**
     * 添加调宿申请
     */
    @Override
    public int addApply(AdjustRoom adjustRoom) {
        adjustRoom.setId(null);
        int insert = adjustRoomMapper.insert(adjustRoom);
        return insert;
    }

    /**
     * 查找调宿申请（管理员查询所有）
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<AdjustRoom> qw = new QueryWrapper<>();
        // 支持按学号或姓名搜索
        if (search != null && !search.trim().isEmpty()) {
            qw.and(wrapper -> wrapper.like("username", search.trim()).or().like("name", search.trim()));
        }
        qw.orderByDesc("apply_time");
        Page orderPage = adjustRoomMapper.selectPage(page, qw);
        return orderPage;
    }

    /**
     * 删除调宿申请
     */
    @Override
    public int deleteAdjustment(Integer id) {
        int i = adjustRoomMapper.deleteById(id);
        return i;
    }


    /**
     * 更新调宿申请
     */
    @Override
    public int updateApply(AdjustRoom adjustRoom) {
        // 如果状态是英文，转换为中文状态（数据库表目前只支持中文状态）
        if (adjustRoom.getState() != null) {
            String chineseState = convertStateToChinese(adjustRoom.getState());
            adjustRoom.setState(chineseState);
        }
        int i = adjustRoomMapper.updateById(adjustRoom);
        return i;
    }

    /**
     * 宿管审核调宿申请
     */
    @Override
    public int reviewApply(Integer id, String state) {
        AdjustRoom adjustRoom = new AdjustRoom();
        adjustRoom.setId(id);
        // 将英文状态转换为中文状态（数据库表目前只支持中文状态）
        String chineseState = convertStateToChinese(state);
        adjustRoom.setState(chineseState);
        // 设置处理时间
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        adjustRoom.setFinishTime(now.format(formatter));
        int i = adjustRoomMapper.updateById(adjustRoom);
        return i;
    }
    
    /**
     * 将英文状态转换为中文状态
     */
    private String convertStateToChinese(String state) {
        if (state == null) {
            return "未处理";
        }
        switch (state.toLowerCase()) {
            case "pending":
            case "未处理":
                return "未处理";
            case "approved":
            case "通过":
                return "通过";
            case "rejected":
            case "驳回":
                return "驳回";
            case "in_progress":
            case "处理中":
                return "处理中";
            case "completed":
            case "已完成":
                return "已完成";
            default:
                // 如果已经是中文状态，直接返回
                if (state.equals("未处理") || state.equals("通过") || state.equals("驳回") 
                    || state.equals("处理中") || state.equals("已完成")) {
                    return state;
                }
                return "未处理";
        }
    }

    /**
     * 个人调宿申请查询（学生用）
     */
    @Override
    public Page individualFind(Integer pageNum, Integer pageSize, String search, String username) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<AdjustRoom> qw = new QueryWrapper<>();
        // 必须按用户名精确查询（学生只能查看自己的申请）
        qw.eq("username", username);
        // 如果search不为空，添加额外的搜索条件（搜索学号或姓名）
        if (search != null && !search.trim().isEmpty()) {
            qw.and(wrapper -> wrapper
                    .like("username", search.trim())
                    .or()
                    .like("name", search.trim())
            );
        }
        // 按申请时间倒序排列（最新的在前）
        qw.orderByDesc("apply_time");
        Page orderPage = adjustRoomMapper.selectPage(page, qw);
        return orderPage;
    }

    /**
     * 管理员查询审核通过的调宿申请
     */
    @Override
    public Page findApproved(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<AdjustRoom> qw = new QueryWrapper<>();
        if (search != null && !search.trim().isEmpty()) {
            qw.and(wrapper -> wrapper.like("username", search.trim()).or().like("name", search.trim()));
        }
        // 只查询审核通过状态的申请（数据库存储的是中文状态"通过"）
        qw.eq("state", "通过");
        qw.orderByDesc("apply_time");
        Page orderPage = adjustRoomMapper.selectPage(page, qw);
        return orderPage;
    }

    /**
     * 宿管查询管辖范围内的调宿申请
     */
    @Override
    public Page findForDormManager(Integer pageNum, Integer pageSize, String search, Integer dormBuildId) {
        Page<AdjustRoom> page = new Page<>(pageNum, pageSize);
        QueryWrapper<AdjustRoom> qw = new QueryWrapper<>();

        // 只查询当前房间属于该宿舍楼的调宿申请（即搬出本楼的申请）
        qw.inSql("currentroom_id", "SELECT dormroom_id FROM dorm_room WHERE dormbuild_id = " + dormBuildId);

        // 如果有搜索条件，按用户名或姓名模糊匹配
        if (search != null && !search.trim().isEmpty()) {
            qw.and(wrapper -> wrapper.like("username", search.trim()).or().like("name", search.trim()));
        }

        qw.orderByDesc("apply_time");
        return adjustRoomMapper.selectPage(page, qw);
    }

}
