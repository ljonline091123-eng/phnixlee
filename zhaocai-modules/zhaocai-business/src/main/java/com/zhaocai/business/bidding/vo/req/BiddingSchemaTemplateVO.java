package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/18 15:23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingSchemaTemplateVO", description = "采购方案评分模板关联关系VO")
public class BiddingSchemaTemplateVO implements Serializable {
    private static final long serialVersionUID = -1549325737535549288L;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 评分模板id */
    @ApiModelProperty(value =  "评分模板id")
    private Long templateId;

}
