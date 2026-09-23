package com.zhaocai.business.filez.dto;

import lombok.Data;

/**
 * 格式转换请求
 *
 * @author chenming
 * @date 2024-07-30
 */
@Data
public class CovertRequestDTO extends FileZRequestDTO{

    /**
     * 带转换的文件路径
     */
    private String fileUrl;

    /**
     * 目标文件名称，不包含文件后缀
     */
    private String targetFileName;

    /**
     * 目标文件格式
     */
    private String targetFileType;

    /**
     * 水印
     */
    private String watermarkText;

    public CovertRequestDTO(String fileUrl,String targetFileName,String targetFileType) {
        this.fileUrl = fileUrl;
        this.targetFileName = targetFileName;
        this.targetFileType = targetFileType;
    }
}
