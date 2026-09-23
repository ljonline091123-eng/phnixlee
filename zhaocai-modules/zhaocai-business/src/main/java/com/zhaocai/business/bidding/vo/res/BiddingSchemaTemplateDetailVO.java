package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/6/18 15:39
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingSchemaTemplateDetailVO", description = "采购方案评分模板关联关系信息VO")
public class BiddingSchemaTemplateDetailVO {

    @ApiModelProperty(value =  "主键id")
    private Long id;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "评分模板id")
    private Long templateId;

    @ApiModelProperty(value =  "评分模板名称")
    private String templateName;

}
