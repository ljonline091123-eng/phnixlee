package com.zhaocai.archives.main.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 材料档案主对象 mtr_archives
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
@TableName(value = "mtr_archives")
public class MtrArchives implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 材料分类id
     */
    @Excel(name = "材料分类id")
    private String mtrClassId;

    /**
     * 材料编号
     */
    @Excel(name = "材料编号")
    private String mtrCode;

    /**
     * 材料名称
     */
    @Excel(name = "材料名称")
    private String mtrName;

    /**
     * 规格
     */
    @Excel(name = "规格")
    private String specs;

    /**
     * 计量单位
     */
    @Excel(name = "计量单位")
    private String measureUnit;

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
     * 所属分类
     */
    @TableField(exist = false)
    private String mtrClassName;

    /**
     * ids
     */
    @TableField(exist = false)
    private List<String> ids;


}
