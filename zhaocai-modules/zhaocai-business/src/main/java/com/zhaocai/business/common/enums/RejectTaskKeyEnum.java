package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RejectTaskKeyEnum {
    SUBMIT("submit");

    /**
     * 描述
     */
    private final String desc;
}
