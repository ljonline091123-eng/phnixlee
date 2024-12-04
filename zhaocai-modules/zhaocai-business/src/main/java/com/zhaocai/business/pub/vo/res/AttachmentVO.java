package com.zhaocai.business.pub.vo.res;

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
public class AttachmentVO {

    @ApiModelProperty(value = "附件 id")
    private Long id;

    @ApiModelProperty(value =  "文件 url")
    private String fileUrl;

    @ApiModelProperty(value =  "文件名")
    private String fileName;


    public AttachmentVO(String fileName,String fileUrl) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }
}
