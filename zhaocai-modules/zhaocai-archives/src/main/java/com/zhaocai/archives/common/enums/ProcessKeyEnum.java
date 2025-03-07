package com.zhaocai.archives.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessKeyEnum {

    /**
     * 流程Key枚举
     */
    ZHAOCAI_ASSISTANT_MATERIAL("jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_MATERIAL", "副库-材料档案"),

    ZHAOCAI_ASSISTANT_DEVICE("jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_DEVICE", "副库-设备档案"),

    ZHAOCAI_ASSISTANT_LABOUR("jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_LABOUR", "副库-劳务档案"),

    ZHAOCAI_ASSISTANT_SUBCONTRACTING("jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_SUBCONTRACTING", "副库-专业分包档案");


    /**
     * 流程标识
     */
    private final String identifying;
    /**
     * 描述
     */
    private final String desc;
}
