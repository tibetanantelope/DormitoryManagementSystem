package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.entity.AdjustRoom;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

import java.util.Map;

public interface AdjustRoomMapper extends BaseMapper<AdjustRoom> {
    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_execute_adjust_room(#{adjustId, mode=IN, jdbcType=INTEGER}, " +
            "#{resultCode, mode=OUT, jdbcType=INTEGER}, " +
            "#{resultMsg, mode=OUT, jdbcType=VARCHAR})")
    void executeAdjustRoom(Map<String, Object> params);
}
