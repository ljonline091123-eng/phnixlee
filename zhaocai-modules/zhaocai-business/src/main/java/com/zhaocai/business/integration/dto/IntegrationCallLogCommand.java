package com.zhaocai.business.integration.dto;

import lombok.Data;

import java.util.Date;

/** 外部接口调用审计命令，避免业务域依赖历史日志表对象。 */
@Data
public class IntegrationCallLogCommand {
    private Date sendTime;
    private String sendData;
    private String sendUrl;
    private String receiveData;
    private Date receiveTime;
    private String logType;
    private boolean successful;
    private String businessId;
    private String remark;
}
