package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;


/**
 * 流程服务-尝试对流程进行加锁
 */
@Data
public class BpmTryLockRequestDTO extends UnderlyingPlatformBaseDTO{



    private BpmAuditRequestDTO auditReq;
    private BpmDisCardRequestDTO discardReq;
    private BpmRevokeRequestDTO revokeReq;
    private BpmSubmitRequestDTO submitReq;






}
