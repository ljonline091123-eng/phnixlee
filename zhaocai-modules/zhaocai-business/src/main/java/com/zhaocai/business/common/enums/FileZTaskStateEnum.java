package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 联想文档任务
 *
 * @author chenming
 * @date 2024-07-22
 */
@Getter
@AllArgsConstructor
public enum FileZTaskStateEnum {

    IN_EXECUTION(0,"执行中"),
    EXECUTE_SUCCESS(1,"执行成功"),
    EXECUTE_FAILED(2,"执行失败"),

    ;

    private final Integer state;

    private final String desc;

    public boolean equalsState(Integer state) {
        return this.getState().equals(state);
    }
}
