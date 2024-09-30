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
public enum JobTitleEnum {
    /**
     * 工种名称
     */
    JOB_TITLE_ONE("1", "架子工"),
    JOB_TITLE_TWO("2", "钢筋工"),
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
        for (JobTitleEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }
}
