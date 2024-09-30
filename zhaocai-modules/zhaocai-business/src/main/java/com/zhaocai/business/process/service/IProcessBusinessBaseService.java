package com.zhaocai.business.process.service;

import java.util.Map;

/**
 * @author ssy
 * @date 2024/7/30 10:37
 */
public interface IProcessBusinessBaseService {

    void processStart(Map<String, Object> variables);

    void processAuditPass(Map<String, Object> variables);

    default void processAuditFreedom(Map<String, Object> variables){}

    default void processAuditReject(Map<String, Object> variables){}

    default void processAuditRevoke(Map<String, Object> variables){}
}
