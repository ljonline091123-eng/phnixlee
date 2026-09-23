package com.zhaocai.business.filez.service.dto;


import lombok.Data;

/**
 * 联想文档响应信息详情
 *
 * @author chenming
 * @date 2024-07-23
 */
@Data
public class FileZResponseDetail {

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 默认下载路径
     */
    private String defaultDownloadPath;

    /**
     *
     */
    private String contentId;

    /**
     *
     */
    private String filename;
}
