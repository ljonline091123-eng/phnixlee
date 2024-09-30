package com.zhaocai.business.pub.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 供应商附件
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRequestVO {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件 URL")
    private String fileUrl;
}
