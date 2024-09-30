package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/28 11:01
 */
@Data
@ApiModel(value = "ExpertEvalRecordVO", description = "专家评分记录数据VO")
public class ExpertEvalRecordVO implements Serializable {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "投标单信息id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "商务评分")
    private BigDecimal busScore;

    @ApiModelProperty(value =  "技术评分")
    private BigDecimal techScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value =  "评标时间")
    private Date evaTime;

    @ApiModelProperty(value =  "评标意见")
    private String evaOpinion;

}
