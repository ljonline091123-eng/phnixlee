package com.zhaocai.business.filez.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 文件下载请求DTO
 *
 * @author chenming
 * @date 2024-07-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileZDownloadRequestDTO extends FileZRequestDTO {

    /**
     * 任务 id
     */
    @NotBlank(message = "任务 id 不能为空")
    private String taskId;

    /**
     * 结果 id
     */
    @NotBlank(message = "结果 id 不能为空")
    private String contentId;

    /**
     * 文件名
     */
    private String fileName;
}
