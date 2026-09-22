package com.zhaocai.business.bidding.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 中标候选人及其排序结果。 */
@Getter
@Setter
@ApiModel("中标候选人")
@TableName("tb_award_candidate")
public class AwardCandidate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("定标决策ID")
    private Long decisionId;

    @ApiModelProperty("投标版本ID")
    private Long submissionVersionId;

    @ApiModelProperty("供应商ID")
    private Long vendorId;

    @TableField("`rank`")
    @ApiModelProperty("综合排名")
    private Integer rank;

    @ApiModelProperty("综合得分")
    private BigDecimal totalScore;

    @ApiModelProperty("推荐理由")
    private String recommendationReason;
}
