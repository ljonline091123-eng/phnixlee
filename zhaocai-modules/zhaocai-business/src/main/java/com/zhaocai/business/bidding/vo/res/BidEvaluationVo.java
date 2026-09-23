package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BidEvaluationVo", description = "评分汇总列表数据VO")
public class BidEvaluationVo implements  Serializable {


    private static final long serialVersionUID = 399181303516855059L;


    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;



    @ApiModelProperty(value =  "得分")
    private BigDecimal score;

    @ApiModelProperty(value =  "专家打分列表")
    private List<BidEvaluationExpertScoreVo> bidEvaluationExpertScoreVoList;



}
