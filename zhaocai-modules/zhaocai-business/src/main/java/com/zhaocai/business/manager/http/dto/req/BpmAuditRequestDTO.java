package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;


/**
 * 流程服务-审批接口
 */
@Data
public class BpmAuditRequestDTO extends UnderlyingPlatformBaseDTO{


    /**
     * 业务内容
     */
    private String businessContent;


    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;

    /**
     * 当前任务ID
     */
    private String curTaskId;

    /**
     * 下一个审批用户id（审批通过时生效；多个用户ID使用英文逗号分隔）
     */
    private String nextAuditUserId;

    /**
     * 批注信息
     */
    private String operateComment;

    /**
     * 是否审批通过
     */
    private boolean pass;

    /**
     * 流程id
     */
    private String processId;

    /**
     * 驳至任务标识（审批驳回时必须）
     */
    private String rejectTaskKey;
}
