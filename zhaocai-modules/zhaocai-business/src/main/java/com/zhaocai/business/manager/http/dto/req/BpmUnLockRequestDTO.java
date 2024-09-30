package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;


/**
 * 流程服务-尝试对流程进行加锁
 */
@Data
public class BpmUnLockRequestDTO extends UnderlyingPlatformBaseDTO{


    /**
     * 流程实例id processId和businessId不能同时为空
     */
    private String processId;


    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;






}
