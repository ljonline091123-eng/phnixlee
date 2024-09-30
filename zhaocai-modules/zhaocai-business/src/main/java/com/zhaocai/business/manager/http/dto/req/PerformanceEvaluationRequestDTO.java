package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

/**
 * 履约评价请求 DTO
 *
 * @author chenming
 * @date 2024-07-12
 */
@Data
public class PerformanceEvaluationRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 合同乙方名称
     */
    private String partbName;

    /**
     * 合同乙方 id
     */
    private String partbId;

    /**
     * 项目id
     */
    private String projectId;

    /**
     * 项目编号
     */
    private String projectCode;

}
