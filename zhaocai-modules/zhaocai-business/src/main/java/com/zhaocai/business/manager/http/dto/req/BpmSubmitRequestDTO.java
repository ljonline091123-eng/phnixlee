package com.zhaocai.business.manager.http.dto.req;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


/**
 * 流程服务-提交接口
 */
@Data
public class BpmSubmitRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 业务内容
     */
    private String businessContent;

    /**
     * 业务ID，processId和businessId不能同时为空
     */
    private String businessId;

    /**
     * 业务标题
     */
    private String businessTitle;

    /**
     * 是否需要发送订单消息
     */
    private Boolean needDingtalkMsg;

    /**
     * 是否需要发送微信待办
     */
    private Boolean needWechatMsg;

    /**
     * 下一审批用户ID，多个用户ID使用英文逗号分隔
     */
    private String nextAuditUserId;

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

    /**
     * 流程key 必传
     */
    private  String processKey;

    /**
     * 用户自定义信息
     */
    private String userObj;
}
