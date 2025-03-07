package com.zhaocai.archives.api.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 劳务特征值主对象 labor_services_feature_value
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
public class LaborServicesFeatureValueVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    private String id;

    /**
     * 劳务特征项id
     */
    //// @Excel(name = "劳务特征项id")
    private String laborServicesFeatureId;

    /**
     * 特征值名称
     */
    //// @Excel(name = "特征值名称")
    private String featureValueName;

    /**
     * 特征值编号
     */
    //// @Excel(name = "特征值编号")
    private String featureValueCode;

    /**
     * 创建者id
     */
    //// @Excel(name = "创建者id")
    private String createId;

    /**
     * 创建者
     */
    //// @Excel(name = "创建者")
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
    //// @Excel(name = "更新者")
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
    //// @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long valid;

    /**
     * 副库id
     */
    //// @Excel(name = "副库id")
    private Long sonId;

    /**
     * 是否为主库同步数据
     */
    private String isMain;

    /**
     * 状态
     */
    private Long state;

    /**
     * 流程实例 id
     */
    private String wfProcessId;

    /**
     * 流程批次
     */
    private String wfBatch;

    /**
     * 关联主表id
     */
    private String mainId;

    /**
     * 关联主表id
     */
    private String hostId;


    /**
     * 机构编码
     */
    private String organCode;


    /**
     * 部门ID
     */
    private Long deptId;

}
