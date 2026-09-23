package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;


/**
 * 流程服务-弃审接口
 */
@Data
public class BpmDisCardRequestDTO extends UnderlyingPlatformBaseDTO{



    /**
     * 流程实例id processId和businessId不能同时为空
     */
    private String processId;


    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;



    /**
     * 批注信息
     */
    private String operateComment;



    /**
     * 弃审任务id
     */
    private String discardTaskId;





}
