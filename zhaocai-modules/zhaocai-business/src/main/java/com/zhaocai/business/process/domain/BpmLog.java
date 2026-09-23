package com.zhaocai.business.process.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;


/**
 * tb_bpm_log 流程记录
 */
@Data
@Builder
@TableName(value = "tb_bpm_log")
public class BpmLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id",type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 业务 id
     */
    @ApiModelProperty(value = "业务 id")
    private String businessId;

    /**
     * 流程实例 id
     */
    @ApiModelProperty(value = "流程实例 id")
    private String wfProcessId;

    /** 流程类型(提交，审批，撤回，驳回...) */
    @ApiModelProperty(name = "流程类型")
    private String bpmType;

    /** 流程key zhaocai-000-ABC */
    @ApiModelProperty(name = "流程key")
    private String bpmKey;

    /** 提交参数 */
    @ApiModelProperty(name = "提交参数")
    private String bpmParam;

    /** 返回参数 */
    @ApiModelProperty(name = "返回参数")
    private String bpmResponse;

    /** 流程发送地址 */
    @ApiModelProperty(name = "流程发送地址")
    private String bpmUrl;


    /** 创建者 */
    @ApiModelProperty(value = "创建者")
    @TableField(fill = FieldFill.INSERT)/* 自动注入 */
    private String createBy;

    /** 创建人 id */
    @ApiModelProperty(value =  "创建人 id")
    @TableField(fill = FieldFill.INSERT)/* 自动注入 */
    private Long createId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)/* 自动注入 */
    private Date createTime;
}
