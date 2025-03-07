package com.zhaocai.archives.common.vo.res;

import lombok.Data;

@Data
public class CompletedTaskListDTO {

    /**
     * 任务key
     */
    private String taskKey;

    /**
     * 任务名称
     */
    private String taskName;
}
