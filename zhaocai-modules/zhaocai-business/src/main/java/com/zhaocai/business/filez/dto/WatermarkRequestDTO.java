package com.zhaocai.business.filez.dto;

import lombok.Data;

/**
 * 水印请求dto
 *
 * @author chenming
 * @date 2024-07-22
 */
@Data
public class WatermarkRequestDTO extends DocumentUpdateRequestDTO{

    /**
     * 水印文本
     */
    private String text;
}
