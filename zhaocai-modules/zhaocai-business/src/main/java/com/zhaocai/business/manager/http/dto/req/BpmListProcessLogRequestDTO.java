package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;

import java.util.List;


/**
 * 流程服务-撤销接口
 */
@Data
public class BpmListProcessLogRequestDTO extends UnderlyingPlatformBaseDTO{


    /**
     * 流程实例id processId和businessId不能同时为空
     */
    private String processId;


    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;

    /**
     * 流程类型 专家用到了-> {@link com.zhaocai.business.common.enums.ExpertProcessTypeEnum}
     */
    private String processType;

    /**
     * 运行时属性对象
     */
    private List<PropertyListRequestDTO<Object>> propertyList;




}
