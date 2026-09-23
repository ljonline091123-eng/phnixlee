package com.zhaocai.business.bidding.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** 定标决策聚合根，记录决策本身，不混入候选人排名和公示字段。 */
@Getter
@Setter
@ApiModel("定标决策")
@TableName("tb_award_decision")
public class AwardDecision extends BaseEntity {
    private static final long serialVersionUID = 1L;

    public static final int DRAFT = 0;
    public static final int DECIDED = 1;
    public static final int PUBLISHED = 2;
    public static final int CANCELLED = 3;

    @ApiModelProperty("招标公告ID")
    private Long noticeId;

    @ApiModelProperty("采购方案ID")
    private Long schemeId;

    @ApiModelProperty("中选候选人ID")
    private Long selectedCandidateId;

    @ApiModelProperty("决策状态：0草稿、1已定标、2已公示、3已取消")
    private Integer status;

    @ApiModelProperty("定标依据")
    private String decisionBasis;

    @ApiModelProperty("决策人用户ID")
    private Long decidedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("定标时间")
    private Date decidedAt;

    public void decide(Long candidateId, Long operatorId, String basis, Date now) {
        if (candidateId == null) {
            throw new IllegalArgumentException("中选候选人不能为空");
        }
        if (!Integer.valueOf(DRAFT).equals(status)) {
            throw new IllegalStateException("只有草稿状态可以定标");
        }
        selectedCandidateId = candidateId;
        decidedBy = operatorId;
        decisionBasis = basis;
        decidedAt = now;
        status = DECIDED;
    }

    public void markPublished() {
        if (!Integer.valueOf(DECIDED).equals(status)) {
            throw new IllegalStateException("只有已定标的决策才能发布公示");
        }
        status = PUBLISHED;
    }
}
