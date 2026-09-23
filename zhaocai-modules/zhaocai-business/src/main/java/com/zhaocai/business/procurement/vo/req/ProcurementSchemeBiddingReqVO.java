package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案请求
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class ProcurementSchemeBiddingReqVO {

    /**
     * 评分模板id
     */
    @ApiModelProperty(value = "评分模板id")
    private Long evaluationTemplateId;

    /**
     * 招标文件模板id
     */
    @ApiModelProperty(value = "招标文件附件id")
    private Long biddingAttachmentId;

    /**
     * 招标文件模板 id
     */
    @ApiModelProperty(value = "招标文件模板 id")
    private Long biddingTemplateId;

    /**
     * 合同模板id
     */
    @ApiModelProperty(value = "合同模板id")
    private Long contractTemplateId;
}
