package com.zhaocai.business.common.annotations;

import java.lang.annotation.*;

/**
 * 金额格式化字段
 *
 * @author chenming
 * @date 2024-07-08
 */
@Documented
@Target({ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface MoneyFormat {

    /**
     * 字段名
     */
    String filedName() default "";

    /**
     * 保留位数<br/>
     * 数量：2位
     * 金额：4位
     */
    int scale() default 4;

    /**
     * 是否补 0
     * @return
     */
    boolean zeroFill() default true;
}
