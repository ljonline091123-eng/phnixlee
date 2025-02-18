package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案其他文件
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
public class ProcurementSchemeOtherFileVO {


    @ApiModelProperty(value = "模板附件 id")
    private Long attachmentId;

    @ApiModelProperty(value = "模板名称")
    private String fileName;

    @ApiModelProperty(value = "模板名称")
    private String fileUrl;

    public ProcurementSchemeOtherFileVO(Long attachmentId, String fileUrl, String fileName) {
        this.attachmentId = attachmentId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }
}
