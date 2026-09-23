package com.zhaocai.business.bidding.vo.req.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/5/31 14:18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingInfoQueryVO", description = "查询投标单信息列表VO")
public class BiddingInfoQueryVO {

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "排除掉投标状态")
    private Integer excludeBiddingStatus;

}
