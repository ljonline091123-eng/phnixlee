package com.zhaocai.business.bidding.vo.req.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/6/3 16:26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingOpenPeopleQueryVO", description = "查询开标人员信息列表VO")
public class BiddingOpenPeopleQueryVO {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "人员id")
    private Long userId;

    @ApiModelProperty(value =  "是否开标（0未开标 1已开标）")
    private Integer isOpen;

}
