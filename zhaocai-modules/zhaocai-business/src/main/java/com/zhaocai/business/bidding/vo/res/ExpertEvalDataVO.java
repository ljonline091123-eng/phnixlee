package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/24 15:02
 */
@Data
@ApiModel(value = "ExpertEvalDataVO", description = "专家评分数据VO")
public class ExpertEvalDataVO implements Serializable {
    private static final long serialVersionUID = -41633875302288295L;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "投标单信息id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "投标供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "专家id")
    private Long expertId;

    @ApiModelProperty(value =  "商务评分")
    private BigDecimal busScore;

    @ApiModelProperty(value =  "技术评分")
    private BigDecimal techScore;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "评标时间")
    private Date evaTime;

    @ApiModelProperty(value =  "评标意见")
    private String evaOpinion;

    @ApiModelProperty(value =  "评标状态")
    private Integer evalStatus;

    @ApiModelProperty(value =  "专家评分详情数据")
    private List<ExpertEvalDetailDataVO> detailDataVOList;


}
