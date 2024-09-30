package com.zhaocai.business.common.annotations;


import java.lang.annotation.*;


/**
 * 防止重复提交
 *
 * @author chenming
 * @date 2024-08-09
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 校验键
     * @return
     */
    String key() default "";

    /**
     * 过期时间，在这个时间内的数据都视为重复数据，单位秒
     * @return
     */
    long expiredTime() default 3;
}
