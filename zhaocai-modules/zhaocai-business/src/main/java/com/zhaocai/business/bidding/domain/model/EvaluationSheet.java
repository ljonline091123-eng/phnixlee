package com.zhaocai.business.bidding.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/** 专家针对一个投标版本提交的评标表。 */
@Getter
@Setter
@ApiModel("评标表")
@TableName("tb_evaluation_sheet")
public class EvaluationSheet extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("评标任务ID")
    private Long assignmentId;

    @ApiModelProperty("投标版本ID")
    private Long submissionVersionId;

    @ApiModelProperty("商务评分")
    private BigDecimal businessScore;

    @ApiModelProperty("技术评分")
    private BigDecimal technicalScore;

    @ApiModelProperty("报价评分")
    private BigDecimal quotationScore;

    @ApiModelProperty("总分")
    private BigDecimal totalScore;

    @ApiModelProperty("评标意见")
    private String opinion;

    @ApiModelProperty("状态：0草稿、1已提交")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submittedAt;

    public void calculateTotalScore() {
        totalScore = zeroIfNull(businessScore)
                .add(zeroIfNull(technicalScore))
                .add(zeroIfNull(quotationScore));
    }

    public void submit(Date now) {
        calculateTotalScore();
        status = 1;
        submittedAt = now;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
