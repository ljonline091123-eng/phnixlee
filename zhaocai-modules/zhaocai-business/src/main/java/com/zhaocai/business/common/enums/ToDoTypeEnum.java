package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ToDoTypeEnum {


    EXAMINE("审批待办"),
    WORK_NOTICE("工作通知");


    private final String desc;

}
