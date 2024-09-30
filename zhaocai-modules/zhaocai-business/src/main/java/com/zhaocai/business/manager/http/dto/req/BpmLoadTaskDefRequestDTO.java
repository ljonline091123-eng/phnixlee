package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;

import java.util.List;


/**
 * 加载定义接口
 */
@Data
public class BpmLoadTaskDefRequestDTO extends UnderlyingPlatformBaseDTO{


    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;

    /**
     * 业务参与机构ID
     */
    private String objectOrgId;

    /**
     * 注册流程标识
     */
    private String processKey;

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
