package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/6/4 15:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EvaluatBidVO", description = "评标操作参数VO")
public class EvaluatBidVO {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

}
