package com.zhaocai.business.process.service;

import java.util.Map;

/**
 * @author ssy
 * @date 2024/7/30 10:37
 */
public interface IProcessBusinessBaseService {

    /** 流程提交 可能是 审批中 也可能是 审批通过 要根据completedFlag状态判断 */
    void processStart(Map<String, Object> variables);

    /** 审批通过，可设置状态为 已完成 */
    void processAuditPass(Map<String, Object> variables);

    /** 驳回到发起人，可设置状态为 保存/自由态 */
    default void processAuditFreedom(Map<String, Object> variables){}

    /** 驳回到中途节点，可以设置状态为 审批中 */
    default void processAuditReject(Map<String, Object> variables){}

    /** 撤回流程，设置由发起人撤回，可以设置状态为 已撤回 */
    default void processAuditRevoke(Map<String, Object> variables){}
}
