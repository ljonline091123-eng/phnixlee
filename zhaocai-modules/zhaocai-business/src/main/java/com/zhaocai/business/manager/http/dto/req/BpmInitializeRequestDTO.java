package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;

import java.util.List;


/**
 * 流程服务-初始化接口
 */
@Data
public class BpmInitializeRequestDTO extends UnderlyingPlatformBaseDTO{


    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;

    /**
     * 业务参与机构ID
     */
    private String objectOrgId;

    /**
     * 批注信息
     */
    private String operateComment;

    /**
     * 流程实例ID，processId和businessId不能同时为空
     */
    private String processId;

    /**
     * 运行时属性对象
     */
    private List<PropertyListRequestDTO> propertyList;

    /**
     * 会话标识
     */
    private String state;

    /**
     * 业务所属机构id
     */
    private String subjectOrgId;
}
