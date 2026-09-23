package com.zhaocai.archives.main.vo.res;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 物料详情列表主对象
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
public class ArchivesDetail  {

    @ApiModelProperty(value = "id")
    private String id;

    @ApiModelProperty(value = "物料 id")
    private String materialsId;

    @ApiModelProperty(value = "物料清单唯一 id")
    private String materialsUniqueId;

    @ApiModelProperty(value = "物料编码")
    private String materialsCode;

    @ApiModelProperty(value = "物料名称")
    private String materialsName;

    @ApiModelProperty(value = "特征值特征项")
    private String specification;

    @ApiModelProperty(value = "计量规则")
    private String measurementRules;

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "基本工作内容")
    private String workContent;

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
    private Date updateTime;


    /**
     * 是否删除
     */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long valid;

    @ApiModelProperty(value = "classId")
    private String classId;


}
