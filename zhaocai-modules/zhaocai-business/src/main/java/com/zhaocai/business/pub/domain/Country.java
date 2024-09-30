package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 国家和地区档案对象 tb_country
 *
 * @author WH
 * @date 2024-07-14
 */
@Data
@TableName(value = "tb_country")
public class Country implements Serializable {
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /** 阿拉伯数字代码 */
    @Excel(name = "阿拉伯数字代码")
    private String arabicNumeralCode;

    /** 中文简称 */
    @Excel(name = "中文简称")
    private String chineseAsName;

    /** 中文全称 */
    @Excel(name = "中文全称")
    private String chineseFullName;

    /** 英文简称 */
    @Excel(name = "英文简称")
    private String englishAsName;

    /** 英文全称 */
    @Excel(name = "英文全称")
    private String englishFullName;

    /** 第三方id */
    @Excel(name = "第三方id")
    private String thridId;

    /** 三字符拉丁字母代码 */
    @Excel(name = "三字符拉丁字母代码")
    private String threeCharCode;

    /** 两字符拉丁字母代码 */
    @Excel(name = "两字符拉丁字母代码")
    private String twoCharCode;


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
