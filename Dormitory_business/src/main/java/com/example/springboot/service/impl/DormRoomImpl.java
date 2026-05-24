package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.JudgeBedName;
import com.example.springboot.entity.AdjustRoom;
import com.example.springboot.entity.DormBed;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.mapper.DormBedMapper;
import com.example.springboot.mapper.DormRoomMapper;
import com.example.springboot.service.DormRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DormRoomImpl extends ServiceImpl<DormRoomMapper, DormRoom> implements DormRoomService {

    @Resource
    private DormRoomMapper dormRoomMapper;

    @Resource
    private DormBedMapper dormBedMapper;

    /**
     * 首页顶部：空宿舍统计。
     * current_capacity 不再存储，改为由 dorm_bed 实时统计。
     */
    @Override
    public int notFullRoom() {
        QueryWrapper<DormRoom> qw = new QueryWrapper<>();
        qw.apply("(SELECT COUNT(*) FROM dorm_bed b WHERE b.dormroom_id = dorm_room.dormroom_id AND b.username IS NOT NULL) < max_capacity");
        return Math.toIntExact(dormRoomMapper.selectCount(qw));
    }

    /**
     * 添加房间，同时初始化床位行。
     */
    @Override
    @Transactional
    public int addNewRoom(DormRoom dormRoom) {
        int insert = dormRoomMapper.insert(dormRoom);
        if (insert == 1) {
            syncBedsForRoom(dormRoom);
        }
        return insert;
    }

    /**
     * 查找房间，并把 dorm_bed 组装成前端原有的床位展示字段。
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<DormRoom> qw = new QueryWrapper<>();
        qw.like("dormroom_id", search);
        Page roomPage = dormRoomMapper.selectPage(page, qw);
        hydrateRooms(roomPage.getRecords());
        return roomPage;
    }

    /**
     * 更新房间基础信息，并同步床位表。
     */
    @Override
    @Transactional
    public int updateNewRoom(DormRoom dormRoom) {
        int i = dormRoomMapper.updateById(dormRoom);
        if (i == 1) {
            syncBedsForRoom(dormRoom);
        }
        return i;
    }

    /**
     * 删除房间。dorm_bed 通过外键级联删除。
     */
    @Override
    public int deleteRoom(Integer dormRoomId) {
        return dormRoomMapper.deleteById(dormRoomId);
    }

    /**
     * 删除床位上的学生信息。
     */
    @Override
    public int deleteBedInfo(String bedName, Integer dormRoomId, int calCurrentNum) {
        Integer bedNo = getBedNo(bedName);
        if (bedNo == null) {
            return 0;
        }
        UpdateWrapper<DormBed> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("dormroom_id", dormRoomId);
        updateWrapper.eq("bed_no", bedNo);
        updateWrapper.set("username", null);
        return dormBedMapper.update(null, updateWrapper);
    }

    /**
     * 查询该学生是否已有床位。
     */
    @Override
    public DormRoom judgeHadBed(String username) {
        DormBed bed = selectBedByUsername(username);
        if (bed == null) {
            return null;
        }
        DormRoom dormRoom = dormRoomMapper.selectById(bed.getDormRoomId());
        hydrateRoom(dormRoom);
        return dormRoom;
    }

    /**
     * 主页住宿人数。
     */
    @Override
    public Long selectHaveRoomStuNum() {
        QueryWrapper<DormBed> qw = new QueryWrapper<>();
        qw.isNotNull("username");
        return dormBedMapper.selectCount(qw);
    }

    /**
     * 获取每栋宿舍学生总人数。
     */
    @Override
    public Long getEachBuildingStuNum(int dormBuildId) {
        QueryWrapper<DormBed> qw = new QueryWrapper<>();
        qw.inSql("dormroom_id", "SELECT dormroom_id FROM dorm_room WHERE dormbuild_id = " + dormBuildId);
        qw.isNotNull("username");
        return dormBedMapper.selectCount(qw);
    }

    /**
     * 根据调宿申请表对 dorm_bed 进行调整。
     */
    @Override
    @Transactional
    public int adjustRoomUpdate(AdjustRoom adjustRoom) {
        DormBed currentBed = selectBed(adjustRoom.getCurrentRoomId(), adjustRoom.getCurrentBedId());
        if (currentBed == null || !adjustRoom.getUsername().equals(currentBed.getUsername())) {
            return -2;
        }
        DormBed targetBed = selectBed(adjustRoom.getTowardsRoomId(), adjustRoom.getTowardsBedId());
        if (targetBed == null || hasText(targetBed.getUsername())) {
            return -1;
        }

        UpdateWrapper<DormBed> clearCurrent = new UpdateWrapper<>();
        clearCurrent.eq("bed_id", currentBed.getBedId());
        clearCurrent.eq("username", adjustRoom.getUsername());
        clearCurrent.set("username", null);
        int result1 = dormBedMapper.update(null, clearCurrent);
        if (result1 != 1) {
            return -2;
        }

        UpdateWrapper<DormBed> setTarget = new UpdateWrapper<>();
        setTarget.eq("bed_id", targetBed.getBedId());
        setTarget.isNull("username");
        setTarget.set("username", adjustRoom.getUsername());
        int result2 = dormBedMapper.update(null, setTarget);
        if (result2 != 1) {
            UpdateWrapper<DormBed> restore = new UpdateWrapper<>();
            restore.eq("bed_id", currentBed.getBedId());
            restore.isNull("username");
            restore.set("username", adjustRoom.getUsername());
            dormBedMapper.update(null, restore);
            return -1;
        }
        return result2;
    }

    /**
     * 检查该房间是否未满。
     */
    @Override
    public DormRoom checkRoomState(Integer dormRoomId) {
        DormRoom dormRoom = dormRoomMapper.selectById(dormRoomId);
        hydrateRoom(dormRoom);
        if (dormRoom == null || dormRoom.getCurrentCapacity() >= dormRoom.getMaxCapacity()) {
            return null;
        }
        return dormRoom;
    }

    /**
     * 检查该房间是否存在。
     */
    @Override
    public DormRoom checkRoomExist(Integer dormRoomId) {
        DormRoom dormRoom = dormRoomMapper.selectById(dormRoomId);
        hydrateRoom(dormRoom);
        return dormRoom;
    }

    /**
     * 检查床位是否为空。
     */
    @Override
    public DormRoom checkBedState(Integer dormRoomId, int bedNum) {
        DormBed dormBed = selectBed(dormRoomId, bedNum);
        if (dormBed == null || hasText(dormBed.getUsername())) {
            return null;
        }
        DormRoom dormRoom = dormRoomMapper.selectById(dormRoomId);
        hydrateRoom(dormRoom);
        return dormRoom;
    }

    private void syncBedsForRoom(DormRoom dormRoom) {
        for (int bedNo = 1; bedNo <= dormRoom.getMaxCapacity(); bedNo++) {
            DormBed bed = selectBed(dormRoom.getDormRoomId(), bedNo);
            String username = getBedUsername(dormRoom, bedNo);
            if (bed == null) {
                bed = new DormBed();
                bed.setDormRoomId(dormRoom.getDormRoomId());
                bed.setBedNo(bedNo);
                bed.setUsername(emptyToNull(username));
                dormBedMapper.insert(bed);
            } else {
                UpdateWrapper<DormBed> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("bed_id", bed.getBedId());
                updateWrapper.set("username", emptyToNull(username));
                dormBedMapper.update(null, updateWrapper);
            }
        }
        clearOverflowEmptyBeds(dormRoom);
    }

    private void clearOverflowEmptyBeds(DormRoom dormRoom) {
        QueryWrapper<DormBed> qw = new QueryWrapper<>();
        qw.eq("dormroom_id", dormRoom.getDormRoomId());
        qw.gt("bed_no", dormRoom.getMaxCapacity());
        qw.isNull("username");
        dormBedMapper.delete(qw);
    }

    private void hydrateRooms(List<DormRoom> rooms) {
        for (DormRoom room : rooms) {
            hydrateRoom(room);
        }
    }

    private void hydrateRoom(DormRoom room) {
        if (room == null) {
            return;
        }
        QueryWrapper<DormBed> qw = new QueryWrapper<>();
        qw.eq("dormroom_id", room.getDormRoomId());
        qw.orderByAsc("bed_no");
        List<DormBed> beds = dormBedMapper.selectList(qw);
        int currentCapacity = 0;
        for (DormBed bed : beds) {
            if (hasText(bed.getUsername())) {
                currentCapacity++;
            }
            setBedUsername(room, bed.getBedNo(), bed.getUsername());
        }
        room.setCurrentCapacity(currentCapacity);
    }

    private DormBed selectBed(Integer dormRoomId, Integer bedNo) {
        if (dormRoomId == null || bedNo == null) {
            return null;
        }
        QueryWrapper<DormBed> qw = new QueryWrapper<>();
        qw.eq("dormroom_id", dormRoomId);
        qw.eq("bed_no", bedNo);
        return dormBedMapper.selectOne(qw);
    }

    private DormBed selectBedByUsername(String username) {
        QueryWrapper<DormBed> qw = new QueryWrapper<>();
        qw.eq("username", username);
        return dormBedMapper.selectOne(qw);
    }

    private Integer getBedNo(String bedName) {
        if (JudgeBedName.getBedName(1).equals(bedName)) {
            return 1;
        }
        if (JudgeBedName.getBedName(2).equals(bedName)) {
            return 2;
        }
        if (JudgeBedName.getBedName(3).equals(bedName)) {
            return 3;
        }
        if (JudgeBedName.getBedName(4).equals(bedName)) {
            return 4;
        }
        return null;
    }

    private String getBedUsername(DormRoom room, int bedNo) {
        switch (bedNo) {
            case 1:
                return room.getFirstBed();
            case 2:
                return room.getSecondBed();
            case 3:
                return room.getThirdBed();
            case 4:
                return room.getFourthBed();
            default:
                return null;
        }
    }

    private void setBedUsername(DormRoom room, int bedNo, String username) {
        switch (bedNo) {
            case 1:
                room.setFirstBed(username);
                break;
            case 2:
                room.setSecondBed(username);
                break;
            case 3:
                room.setThirdBed(username);
                break;
            case 4:
                room.setFourthBed(username);
                break;
            default:
                break;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value : null;
    }
}
