package com.zhaocai.archives.api.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备分类主对象 device_class
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
public class DeviceClassVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    private String id;

    /**
     * 设备分类编码
     */
    // @Excel(name = "设备分类编码")
    private String deviceClassCode;

    /**
     * 设备分类名称
     */
    // @Excel(name = "设备分类名称")
    private String deviceClassName;

    /**
     * 设备分类下级类型 1特征项 2具体设备
     */
    // @Excel(name = "设备分类下级类型 1特征项 2具体设备")
    private String deviceClassType;

    /**
     * 计量单位
     */
    // @Excel(name = "计量单位")
    private String measureUnit;

    /**
     * 交易标的物编号
     */
    // @Excel(name = "交易标的物编号")
    private String subjectMatterCode;

    /**
     * 交易标的物名称
     */
    // @Excel(name = "交易标的物名称")
    private String subjectMatterName;

    /**
     * 是否交易标的物 true是 false不是
     */
    // @Excel(name = "是否交易标的物 true是 false不是")
    private Integer subjectMatter;

    /**
     * 分类层级
     */
    // @Excel(name = "分类层级")
    private String classLevel;

    /**
     * 分类层级
     */
    // @Excel(name = "分类层级")
    private String classLevelCd;

    /**
     * 父节点
     */
    // @Excel(name = "父节点")
    private String parentId;

    /**
     * 创建者id
     */
    // @Excel(name = "创建者id")
    private String createId;

    /**
     * 创建者
     */
    // @Excel(name = "创建者")
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
    // @Excel(name = "更新者")
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
    // @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long valid;

    /**
     * 副库id
     */
    // @Excel(name = "副库id")
    private Long sonId;


    private String isMain;


    private Long state;


    private String wfProcessId;


    private String wfBatch;


    private String mainId;


    private String hostId;


    private String organCode;

}
