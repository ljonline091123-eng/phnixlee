package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案模板
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
public class ProcurementSchemeTemplateVO {

    @ApiModelProperty(value = "模板 id")
    private Long templateId;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "模板附件 id")
    private Long attachmentId;

    @ApiModelProperty(value = "模板名称")
    private String fileName;

    @ApiModelProperty(value = "模板名称")
    private String fileUrl;

    public ProcurementSchemeTemplateVO(Long templateId,String templateName) {
        this.templateId = templateId;
        this.templateName = templateName;
    }

    public ProcurementSchemeTemplateVO(Long attachmentId,String fileUrl,String fileName) {
        this.attachmentId = attachmentId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }
}
