package com.zhaocai.business.receipt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author cff
 * @date 2024/09/10
 */
@Getter
@AllArgsConstructor
public enum ProcessStatusEnum {
    /**
     * 流程状态
     */
    PROCESS_STATUS_ZERO("0", "自由态"),
    PROCESS_STATUS_ONE("1", "审批中"),
    PROCESS_STATUS_TWO("2", "被驳回"),
    PROCESS_STATUS_THREE("3", "已撤销"),
    PROCESS_STATUS_FOUR("4", "已完成"),
    ;

    private final String state;

    private final String desc;


    /**
     * 根据编码查询枚举
     *
     * @param state
     * @return
     */
    public static String getValueByCode(String state) {
        if (Objects.isNull(state)) {
            return null;
        }
        for (ProcessStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
