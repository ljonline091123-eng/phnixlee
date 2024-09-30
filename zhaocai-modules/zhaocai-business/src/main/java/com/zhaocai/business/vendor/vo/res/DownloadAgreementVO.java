package com.zhaocai.business.vendor.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.InputStream;

/**
 * 下载合同 VO
 *
 * @author chenming
 * @date 2024-07-05
 */
@Data
public class DownloadAgreementVO {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件流")
    private InputStream fileStream;
}
