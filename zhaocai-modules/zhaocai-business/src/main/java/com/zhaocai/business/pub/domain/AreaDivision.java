package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行政区划对象 tb_area_division
 *
 * @author WH
 * @date 2024-07-12
 */
@Data
@TableName(value = "tb_area_division")
public class AreaDivision implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 区划名称
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 区划名称
     */
    @ApiModelProperty(value = "区划名称")
    private String areaName;

    /**
     * 行政区划编码
     */
    @ApiModelProperty(value = "行政区划编码")
    private String areaCode;

    /**
     * 父级行政区划编码
     */
    @ApiModelProperty(value = "父级行政区划编码")
    private String parentCode;

    /**
     * 父级区划名称
     */
    @ApiModelProperty(value = "父级区划名称")
    private String parentName;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级")
    private Integer areaLevel;

    /**
     * 层级名称
     */
    @ApiModelProperty(value = "层级名称")
    private String levelAlias;

    /**
     * 显示排序
     */
    @ApiModelProperty(value = "显示排序")
    private String orderNum;

    /**
     * 数据来源
     */
    @ApiModelProperty(value = "数据来源")
    private String origin;

    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者")
    @TableField(fill = FieldFill.INSERT)
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
    @ApiModelProperty(value = "更新者")
    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField("del_flag")
    @TableLogic//逻辑删除注解
    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String delFlag;

    /**
     * 备注
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "备注")
    private String remark;

}
