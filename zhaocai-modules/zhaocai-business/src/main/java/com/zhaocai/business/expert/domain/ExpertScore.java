package com.zhaocai.business.expert.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 专家评分对象 tb_expert_score
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_expert_score")
public class ExpertScore extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 投标单信息id */
    @ApiModelProperty(value =  "投标单信息id")
    private Long biddingInfoId;

    /** 投标供应商id */
    @ApiModelProperty(value =  "投标供应商id")
    private Long vendorId;

    /** 专家id */
    @ApiModelProperty(value =  "专家id")
    private Long expertId;

    /** 商务评分 */
    @ApiModelProperty(value =  "商务评分")
    private BigDecimal busScore;

    /** 技术评分 */
    @ApiModelProperty(value =  "技术评分")
    private BigDecimal techScore;

    /** 评标时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value =  "评标时间")
    private Date evaTime;

    /** 评标意见 */
    @ApiModelProperty(value =  "评标意见")
    private String evaOpinion;

    /** 评标状态（0未评标 1已评标） */
    @ApiModelProperty(value =  "评标状态")
    private Integer evalStatus;
}
