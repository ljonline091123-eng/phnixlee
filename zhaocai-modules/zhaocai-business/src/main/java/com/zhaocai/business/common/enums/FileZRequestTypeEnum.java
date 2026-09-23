package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpMethod;

/**
 * 联想文档请求类型枚举
 *
 * @author chenming
 * @date 2024-07-24
 */
@Getter
@AllArgsConstructor
public enum FileZRequestTypeEnum {

    APPLY_WATERMARK("applyWatermark","/content/update","文档加水印",HttpMethod.POST),
    DOWNLOAD("download","/download","文件下载",HttpMethod.GET),
    UPDATE_BOOKMARK_REF("UpdateBookmarkRef","/content/update","书签内容替换",HttpMethod.POST),
    CONVERT("convert","/convert","格式转换",HttpMethod.POST),

    ;

    /**
     * 请求类型
     */
    private final String requestCode;

    /**
     * 请求 url
     */
    private final String requestUrl;

    /**
     * 请求描述
     */
    private final String requestDesc;

    /**
     * 请求方法
     */
    private final HttpMethod httpMethod;
}
