package com.zhaocai.business.common.annotations;

import java.lang.annotation.*;


/**
 * 供应商状态校验
 *
 * @author chenming
 * @date 2024-07-03
 */
@Documented
@Target({ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface VendorStateCheck {

    /**
     * 是否进行管理员校验
     * @return
     */
    boolean checkManager() default  false;
}
