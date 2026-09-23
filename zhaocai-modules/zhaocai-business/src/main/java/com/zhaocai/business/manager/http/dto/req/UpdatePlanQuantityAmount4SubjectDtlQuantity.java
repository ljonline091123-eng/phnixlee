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
public class UpdatePlanQuantityAmount4SubjectDtlQuantity {
    /**
     * 本次增加的使用数量
     */
    private BigDecimal addQuantity;

    /**
     * 本次减少的使用数量
     */
    private BigDecimal reduceQuantity;

    /**
     * 业务Id
     */
    private String businessId;

    /**
     * 成本子目唯一 id
     */
    private String subjectDtlUniqueId;

    /**
     * 成本子目+特征项+特征值编码
     */
    private String subjectDtlCode;

    /**
     * 成本科目档案ID
     */
    private String subjectId;

    /**
     * 时间戳
     */
    private Long timeStamp;

    /**
     * 本次增加的使用租赁数量
     */
    private BigDecimal addRentQuantity;


    /**
     * 本次减少的使用租赁数量
     */
    private BigDecimal reduceRentQuantity;

    /**
     * 租赁方式(1-日 2-月租 3-工作量)
     */
    private String rentMode;

    /**
     * 租赁数量
     */
    private BigDecimal rentQuantity;

    /**
     * 租赁时间
     */
    private BigDecimal rentTime;
}
