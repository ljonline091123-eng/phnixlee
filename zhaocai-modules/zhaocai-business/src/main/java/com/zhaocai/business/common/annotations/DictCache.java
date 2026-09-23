package com.zhaocai.business.common.annotations;


import com.zhaocai.business.common.enums.DictBizEnum;

import java.lang.annotation.*;

/**
 * dict缓存
 *
 * @author chenming
 * @date 2024/06/03
 */
@Documented
@Target({ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DictCache {

    /**
     * 枚举
     * @return
     */
    DictBizEnum  dictBizEnum();

    /**
     * 字段值
     * @return
     */
    String filedName() default "";
}
