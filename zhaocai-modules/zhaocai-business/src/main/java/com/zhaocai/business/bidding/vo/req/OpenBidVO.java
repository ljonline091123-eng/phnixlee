package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/3 17:05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "OpenBidVO", description = "开标人员开标VO")
public class OpenBidVO implements Serializable {
    private static final long serialVersionUID = 148078826969791325L;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "人员id")
    private Long userId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

}
