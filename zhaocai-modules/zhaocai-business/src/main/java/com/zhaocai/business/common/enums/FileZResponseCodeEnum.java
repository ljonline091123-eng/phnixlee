package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 联想文档响应 code 枚举
 *
 * @author chenming
 * @date 2024-07-23
 */
@Getter
@AllArgsConstructor
public enum FileZResponseCodeEnum {

    CONVERT_SUCCESS_NOTIFY("ConvertSuccessNotify","转换成功通知"),
    CONVERT_FAIL_NOTIFY("convertFailNotify","转换失败通知"),
    TASK_SUCCESS_NOTIFY("TaskSuccessNotify","任务成功通知"),
    TASK_FAIL_NOTIFY("TaskFailNotify","任务失败通知"),
    OK("Ok","请求成功"),

    ;

    private String code;

    private String desc;

    /**
     * 是否成功
     * @param code
     * @return
     */
    public static Boolean isSuccess(String code) {
        return CONVERT_SUCCESS_NOTIFY.getCode().equals(code) || TASK_SUCCESS_NOTIFY.getCode().equals(code) ||
                OK.getCode().equals(code);
    }
}
