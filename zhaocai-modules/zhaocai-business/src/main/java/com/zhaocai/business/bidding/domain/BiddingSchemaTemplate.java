package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 采购方案评分模板关联关系对象 tb_bidding_schema_template
 *
 * @author WH
 * @date 2024-06-18
 */
@Getter
@Setter
@TableName(value = "tb_bidding_schema_template")
public class BiddingSchemaTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 评分模板id */
    @ApiModelProperty(value =  "评分模板id")
    private Long templateId;

}
