package com.zhaocai.business.common.utils;


import com.zhaocai.common.core.utils.NumberUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具类
 *
 * @author chenming
 * @date 2024-09-03
 */
public class AmountCalUtil {

    /**
     * 金额相加
     * @param amount
     * @return
     */
    public static BigDecimal addAmount(BigDecimal refAmount,BigDecimal... amount) {
        BigDecimal total = NumberUtil.add(amount);

        return total.setScale(getScale(refAmount),RoundingMode.HALF_UP);
    }

    /**
     * 计算不含税单价
     * @param unitPriceInclTax
     * @param taxRate
     * @return
     */
    public static BigDecimal calUnitPriceExclTax(BigDecimal unitPriceInclTax, BigDecimal taxRate) {
        BigDecimal calTaxRate = NumberUtil.add(new BigDecimal("1"),NumberUtil.divide(taxRate,new BigDecimal("100")));
        return NumberUtil.divide(unitPriceInclTax,calTaxRate,getScale(unitPriceInclTax));
    }

    /**
     * 计算含税总价 = 数量 * 含税单价，保留 2 位，多余部分舍弃
     * @param amount
     * @param unitPriceInclPrice
     * @return
     */
    public static BigDecimal calTotalAmountInclTax(BigDecimal amount, BigDecimal unitPriceInclPrice) {
        BigDecimal totalAmount = NumberUtil.multiply(amount,unitPriceInclPrice);

        // 保留 2 位小数，多余部分舍弃
        return totalAmount.setScale(2,RoundingMode.DOWN);
    }

    /**
     * 计算不含税总价 = 含税金额 / (1 + 税率) ，保留两位小数，多余部分舍弃
     * @param totalAmountInclTax
     * @param taxRate
     * @return
     */
    public static BigDecimal calTotalAmountExclTax(BigDecimal totalAmountInclTax,BigDecimal taxRate) {
        BigDecimal calTaxRate = NumberUtil.add(new BigDecimal("1"),NumberUtil.divide(taxRate,new BigDecimal("100")));

        BigDecimal totalAmountExclTax = NumberUtil.divide(totalAmountInclTax,calTaxRate,5);
        return totalAmountExclTax.setScale(2,RoundingMode.DOWN);
    }


    /**
     * 计算含税单价 = 基价 * (1 + 浮动率) ，保留两位小数，多余部分舍弃
     * @param basePrice
     * @param floatPriceRate
     * @return
     */
    public static BigDecimal calTotalAmountIncTax(BigDecimal basePrice,BigDecimal floatPriceRate) {
        BigDecimal calTaxRate = NumberUtil.add(new BigDecimal("1"),NumberUtil.divide(floatPriceRate,new BigDecimal("100")));

        BigDecimal totalAmountExclTax = NumberUtil.multiply(basePrice,calTaxRate,5);
        return totalAmountExclTax.setScale(2,RoundingMode.DOWN);
    }

    /**
     * 计算税额 = 含税单价 - 不含税搭建，保留 2 位小数
     * @param amountInclTax
     * @param amountExclTax
     * @return
     */
    public static BigDecimal calTaxAmount(BigDecimal amountInclTax,BigDecimal amountExclTax) {
       BigDecimal result = NumberUtil.subtract(amountInclTax,amountExclTax);

        return result.setScale(2,RoundingMode.DOWN);
    }

    /**
     * 获取小数部分 <br/>
     * 1. 当 value 无小数位，仅有1位或2位小数时，计算出来的含税单价最多保留2位小数<br/>
     * 2. 当 value 有3位或4位小数时，计算出来的含税单价保留最多4位小数
     * @param value
     * @return
     */
    private static Integer getScale(BigDecimal value) {
        int scale = 0;
        if (value != null) {
            scale = value.scale();
        }

        if (scale <= 2) {
            return 2;
        }
        return 4;
    }
}
