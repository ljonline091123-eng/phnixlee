package com.zhaocai.business.filez.vo.req;

import lombok.Data;

/**
 * 联想文档回调 vo
 *
 * @author chenming
 * @date 2024-07-22
 */
@Data
public class FileZCallBackVO {

    /**
     * 任务 id
     */
    private String taskId;

    /**
     * 任务结果码
     */
    private String code;

    /**
     * 详细信息
     */
    private FileZCallBackDetailVO detail;
}
