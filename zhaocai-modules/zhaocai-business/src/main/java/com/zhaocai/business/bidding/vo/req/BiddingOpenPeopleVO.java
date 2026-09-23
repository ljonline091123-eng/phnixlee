package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/3 14:11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingOpenPeopleVO", description = "开标人员信息VO")
public class BiddingOpenPeopleVO implements Serializable {
    private static final long serialVersionUID = -6680728154995020438L;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "人员id")
    private Long userId;

    @ApiModelProperty(value =  "人员姓名")
    private String userName;

    @ApiModelProperty(value =  "第三方待办跳转地址")
    private String redirectUrl;

}
