package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BidEvaluationExpertScoreVo", description = "评分汇总专家列表数据VO")
public class BidEvaluationExpertScoreVo implements  Serializable {


    private static final long serialVersionUID = 399181303516855059L;


    @ApiModelProperty(value =  "专家id")
    private Long expertId;

    @ApiModelProperty(value =  "专家名称")
    private String expertName;

    @ApiModelProperty(value =  "得分")
    private BigDecimal score;

    /** 商务评分 */
    @ApiModelProperty(value =  "商务评分")
    private BigDecimal busScore;

    /** 技术评分 */
    @ApiModelProperty(value =  "技术评分")
    private BigDecimal techScore;

    @ApiModelProperty(value =  "专家id")
    private Long biddingInfoId;




}
