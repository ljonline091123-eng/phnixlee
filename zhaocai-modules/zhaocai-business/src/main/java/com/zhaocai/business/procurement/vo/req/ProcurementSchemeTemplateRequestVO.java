package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案模板请求
 *
 * @author chenming
 * @date 2024/06/03
 */
@Data
public class ProcurementSchemeTemplateRequestVO {

    @ApiModelProperty(value = "模板 id")
    private Long templateId;

    @ApiModelProperty(value = "文件 URL")
    private String fileUrl;

    @ApiModelProperty(value = "文件名称")
    private String fileName;
}
