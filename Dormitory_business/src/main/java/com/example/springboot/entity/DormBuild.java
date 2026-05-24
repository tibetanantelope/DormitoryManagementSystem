package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 宿舍楼
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

@TableName(value = "dorm_build")
public class DormBuild {

    @TableField(exist = false)
    private Integer id;
    @TableId("dormbuild_id")
    private int dormBuildId;
    @TableField("dormbuild_name")
    private String dormBuildName;
    @TableField("dormbuild_type")
    private String dormBuildType;
    @TableField("dormbuild_detail")
    private String dormBuildDetail;
}
