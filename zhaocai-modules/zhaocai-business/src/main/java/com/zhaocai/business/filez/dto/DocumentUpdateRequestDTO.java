package com.zhaocai.business.filez.dto;

import lombok.Data;

/**
 * 联想文档内容修改请求 dto
 *
 * @author chenming
 * @date 2024-07-24
 */
@Data
public class DocumentUpdateRequestDTO extends FileZRequestDTO{
    /**
     * 操作文件
     */
    private String fileUrl;

    /**
     * 操作文件名
     */
    private String fileName;
}
