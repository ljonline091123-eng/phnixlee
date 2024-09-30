package com.zhaocai.business.filez.service.dto;

import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.common.core.utils.file.FileUtils;
import lombok.Data;

/**
 * 格式转换请求 dto
 *
 * @author chenming
 * @date 2024-07-29
 */
@Data
public class ConvertRequestDTO extends FileZRequest{
    /**
     * 文件下载地址
     */
    private String fileUrl;

    /**
     * 包含后缀的文件名
     */
    private String filename;

    /**
     * 包含后缀的目标文件名
     */
    private String targetFilename;

    /**
     * 三方token类型，值为cookie或者header
     */
    private String tokenType;

    /**
     * 三方token值，zOffice下载文件或回调通知时回传
     */
    private String tokenValue;

    /**
     * 回调地址。任务转换结束后zOffice回调通知状态
     */
    private String callback;

    /**
     * 文件内容唯一标识
     */
    private String uniqueId;

    /**
     * 平铺⽔印配置
     */
    private TiledWatermark tiledWatermark;

    public ConvertRequestDTO(String fileUrl) {
        String minioUrl = EnvironmentUtil.getProperty("file-z.minio-url");
        String minioUrlReplace = EnvironmentUtil.getProperty("file-z.minio-url-replace");

        this.fileUrl = fileUrl.replace(minioUrl,minioUrlReplace);
        this.filename = FileUtils.getName(fileUrl);
    }
}
