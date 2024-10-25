package com.zhaocai.business.bidding.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;


@Getter
@AllArgsConstructor
public enum VendorMsgStatusEnum {

    /**
     * 消息类型
     */

    TENDER(1, "招标数据"),
    CERT(2, "法人授权书"),

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
        for (VendorMsgStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }

}
