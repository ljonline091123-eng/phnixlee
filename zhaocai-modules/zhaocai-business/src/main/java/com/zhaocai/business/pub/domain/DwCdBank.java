package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 支行信息
 *
 * @author cs
 * @date 2024-11-20
 */
@Data
@TableName(value = "dw_cd_bank")
@EqualsAndHashCode(callSuper = true)
public class DwCdBank extends PageRecive implements Serializable {
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /** 银行名称 */
    @Excel(name = "银行名称")
    private String parentName;

    /** 银联号 */
    @Excel(name = "银联号")
    private String code;

    /** 部门ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 排序号 */
    @Excel(name = "排序号")
    private Long orderNum;



    /** dr */
    @Excel(name = "dr")
    private String dr;

    /** 主键ID */
    @Excel(name = "主键ID")
    private String internalId;

    /** 支行名称 */
    @Excel(name = "支行名称")
    private String name;


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
     * 备注
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "备注")
    private String remark;

}
