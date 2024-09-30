package com.zhaocai.business.bidding.vo.req.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/6/4 11:50
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingQuotationQueryVO", description = "查询投标单信息报价列表VO")
public class BiddingQuotationQueryVO {

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

}
