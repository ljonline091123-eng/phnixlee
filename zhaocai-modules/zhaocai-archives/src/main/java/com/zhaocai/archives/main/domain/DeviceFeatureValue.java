package com.zhaocai.archives.main.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备特征值主对象 device_feature_value
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
@TableName(value = "device_feature_value")
public class DeviceFeatureValue implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 设备特征项id
     */
    @Excel(name = "设备特征项id")
    private String deviceFeatureId;

    /**
     * 特征值名称
     */
    @Excel(name = "特征值名称")
    private String featureValueName;

    /**
     * 特征值编号
     */
    @Excel(name = "特征值编号")
    private String featureValueCode;

    /**
     * 创建者id
     */
    @Excel(name = "创建者id")
    private String createId;

    /**
     * 创建者
     */
    @Excel(name = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者
     */
    @Excel(name = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;


    /**
     * $column.columnComment
     */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long valid;

    /**
     * 副库id
     */
    @Excel(name = "副库id")
    private Long sonId;

    /**
     * 是否同步中台0否1是2需要更新
     */
    @Excel(name = "是否同步中台")
    private String isTb;

}
