package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/4 15:24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingEvaluatExpertListVO", description = "评标专家人员信息列表VO")
public class BiddingEvaluatExpertListVO implements Serializable {
    private static final long serialVersionUID = 8696042258497944669L;

    @ApiModelProperty(value =  "评标专家人员信息表主键id")
    private Long id;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "专家人员id")
    private Long expertId;

    @ApiModelProperty(value =  "专家人员姓名")
    private String expertName;

    @ApiModelProperty(value =  "专家是否完成评标（0未评标 1已评标）")
    private Integer evalStatus;

}
