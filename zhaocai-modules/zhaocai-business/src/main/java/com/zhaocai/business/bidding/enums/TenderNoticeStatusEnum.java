package com.zhaocai.business.bidding.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author ssy
 * @date 2024/5/27 17:02
 */
@Getter
@AllArgsConstructor
public enum TenderNoticeStatusEnum {

    /**
     * 招标公告-招标阶段状态
     */

    ABANDON_BID(0, "废标"),
    TENDER_ISSUE(1, "发布"),
    BID_OPENING(2, "开标"),
    EVALUATION_BID(3, "评标"),
    @Deprecated
    SECOND_NEG(4, "二次洽商"),
    CALI_REPORT(5, "定标"),
    WINNING_BID(6, "中标公示"),
    RESULT_RELEASE(7, "结果发布"),
    COMPLETE(8, "完成"),

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
        for (TenderNoticeStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }

}
