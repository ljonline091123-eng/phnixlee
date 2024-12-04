package com.zhaocai.business.agreement.enums;

import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 结算阶段settlementStage枚举值 数据库字典值settlement_stage大汉的字典列表{@link UnderlingPlatformUrlEnum#DICT_LIST_MAP}
 * Time:2024/12/4 上午9:14
 * */
@Getter
@AllArgsConstructor
public enum SettlementStageStatusEnum {

    /**
     * 结算阶段settlement_stage 状态 [其他、过程结算、完工结算、最终结算]
     */
    OTHER(1, "其他"),
    PROCESS_SETTLEMENT(2, "过程结算"),
    COMPLETION_SETTLEMENT(3, "完工结算"),
    FINAL_SETTLEMENT(4, "最终结算"),
    ;

    private final Integer state;

    private final String desc;

    /**
     * 根据编码查询枚举
     *
     * @param state
     * @return
     */
    public static String getValueByCode(Integer state) {
        if (Objects.isNull(state)) {
            return null;
        }
        for (SettlementStageStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }

}
