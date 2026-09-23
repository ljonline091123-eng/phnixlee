package com.zhaocai.business.bidding.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author ssy
 * @date 2024/5/31 15:27
 */
@Getter
@AllArgsConstructor
public enum BiddingInfoStatusEnum {

    /**
     * 投标单状态
     */

    NOT_BID(0, "未投标"),
    HAVE_BACK(1, "已回标"),
    HAVE_REPEAL(2, "已撤回"),
    HAVE_ABANDON(3, "已废标"),
    OPENED(4, "已开标"),

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
        for (BiddingInfoStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }

}
