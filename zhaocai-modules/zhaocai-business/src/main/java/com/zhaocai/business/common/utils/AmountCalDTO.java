package com.zhaocai.business.common.utils;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 金额计算 dto
 *
 * @author chenming
 * @date 2024-09-03
 */
@Data
public class AmountCalDTO {

    /**
     * 数量
     */
    private BigDecimal count;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

}
