package com.zhaocai.business.filez.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文档操作类型请求参数
 *
 * @author chenming
 * @date 2024-07-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentUpdateRequest extends FileZRequest{

    /**
     * 文件下载地址
     */
    private String fileUrl;

    /**
     * 包含后缀的文件名，目前源文件类型仅支持doc/docx/wps
     */
    private String filename;

    /**
     * 三方token类型，值为cookie或者header
     */
    private String tokenType;

    /**
     * 三方token值，zOffice下载文件或回调通知时回传
     */
    private String tokenValue;

    /**
     * 回调地址。任务结束后zOffice回调通知状态
     */
    private String callback;

    /**
     * 对文档内容操作的有序操作数组
     */
    private List<FileZRequestBaseOps> ops;
}
