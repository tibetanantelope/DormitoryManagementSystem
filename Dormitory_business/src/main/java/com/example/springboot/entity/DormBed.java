package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 床位信息，作为房间和学生的桥接表。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "dorm_bed")
public class DormBed {

    @TableId(value = "bed_id", type = IdType.AUTO)
    private Integer bedId;
    @TableField("dormroom_id")
    private Integer dormRoomId;
    @TableField("bed_no")
    private Integer bedNo;
    @TableField("username")
    private String username;
}
