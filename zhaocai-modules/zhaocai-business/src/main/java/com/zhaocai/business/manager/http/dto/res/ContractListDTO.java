package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同清单dto
 *
 * @author chenming
 * @date 2024-07-12
 */
@Data
public class ContractListDTO {

    /**
     * 合同编码
     */
    private String conCode;

    /**
     * 合同名称
     */
    private String conName;

    /**
     * 累计结算(元)
     */
    private BigDecimal totalTaxSettleAmount;

    /**
     * 实付付款金额（含税）
     */
    private BigDecimal totalActualPaymentAmount;
}
