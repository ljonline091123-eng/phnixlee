package com.zhaocai.business.filez.service.dto;

import lombok.Data;

/**
 * 响应
 *
 * @author chenming
 * @date 2024-07-23
 */
@Data
public class FileZResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务结果码
     */
    private String code;

    /**
     * 异常消息
     */
    private String exceptionMessage;

    /**
     * 详情
     */
    private FileZResponseDetail detail;
}
