package com.zhaocai.business.pub.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FileBeanVo {

    @ApiModelProperty(value = "文件名")
    private String fileUrl;

    @ApiModelProperty(value = "文件 URL")
    private String appCode;

    @ApiModelProperty(value = "文件 URL")
    private String extraParam;

    @ApiModelProperty(value = "文件 URL")
    private String userId;

    @ApiModelProperty(value = "文件 URL")
    private String customerFileId;

    @ApiModelProperty(value = "文件 URL")
    private String fileId;

    @ApiModelProperty(value = "文件 URL")
    private String fileSize;

    private String filename;

}
