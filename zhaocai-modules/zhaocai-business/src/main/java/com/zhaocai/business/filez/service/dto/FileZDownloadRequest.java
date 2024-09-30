package com.zhaocai.business.filez.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件下载请求DTO
 *
 * @author chenming
 * @date 2024-07-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileZDownloadRequest extends FileZRequest{

    /**
     * 任务 id
     */
    private String taskId;

    /**
     * 结果 id
     */
    private String contentId;
}
