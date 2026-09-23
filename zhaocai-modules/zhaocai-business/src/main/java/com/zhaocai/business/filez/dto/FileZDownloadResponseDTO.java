package com.zhaocai.business.filez.dto;

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
public class FileZDownloadResponseDTO extends FileZResponseDTO {

    /**
     * 保存附件 id
     */
    private Long attachmentId;
}
