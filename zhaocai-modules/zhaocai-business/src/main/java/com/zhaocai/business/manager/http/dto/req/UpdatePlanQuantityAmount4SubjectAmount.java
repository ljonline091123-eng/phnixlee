package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

import java.math.BigDecimal;


/**
 * 合约规划的已发生金额
 *
 * @author chenming
 * @date 2024-07-13
 */
@Data
public class UpdatePlanQuantityAmount4SubjectAmount {
    /**
     * 本次增加的金额
     */
    private BigDecimal addAmount;

    /**
     * 业务Id
     */
    private String businessId;

    /**
     * 成本科目档案ID
     */
    private String subjectId;

    /**
     * 本次减少金额
     */
    private BigDecimal reduceAmount;

    /**
     * 时间戳
     */
    private Long timeStamp;
}
