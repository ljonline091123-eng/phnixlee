package com.zhaocai.archives.main.domain;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备档案主对象 device_archives
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
@TableName(value = "device_archives")
public class DeviceArchives implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * $column.columnComment
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;
    /**
     * 设备分类id
     */
    @Excel(name = "设备分类id")
    private String deviceClassId;

    /**
     * 设备编号
     */
    @Excel(name = "设备编号")
    private String deviceCode;

    /**
     * 设备名称
     */
    @Excel(name = "设备名称")
    private String deviceName;

    /**
     * 特征项
     */
    @Excel(name = "特征项")
    private String feature;

    /**
     * 计量单位
     */
    @Excel(name = "计量单位")
    private String measureUnit;

    /**
     * 是否交易标的物 true是 false不是
     */
    @Excel(name = "是否交易标的物 true是 false不是")
    private Integer subjectMatter;

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

    @TableField(exist = false)
    private String deviceClassName;

}
