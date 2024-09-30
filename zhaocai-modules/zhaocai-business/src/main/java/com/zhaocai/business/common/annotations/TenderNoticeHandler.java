package com.zhaocai.business.common.annotations;

import java.lang.annotation.*;

/**
 * 处理招标状态变更
 * @author ssy
 * @date 2024/7/24 19:28
 */
@Documented
@Target({ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface TenderNoticeHandler {

    /**
     * 招标公告id
     */
    String noticeId() default "";

    /**
     *传递参数的对象类型
     */
    Class<?> parameter() default Object.class;

}
