package com.zhaocai.business.bidding.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** 评标任务，将专家主数据与一次具体评标活动分离。 */
@Getter
@Setter
@ApiModel("评标任务")
@TableName("tb_evaluation_assignment")
public class EvaluationAssignment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    public static final int PENDING = 0;
    public static final int IN_PROGRESS = 1;
    public static final int COMPLETED = 2;
    public static final int CANCELLED = 3;

    @ApiModelProperty("招标公告ID")
    private Long noticeId;

    @ApiModelProperty("专家ID")
    private Long expertId;

    @ApiModelProperty("评标角色：1技术、2商务、3综合")
    private Integer roleType;

    @ApiModelProperty("任务状态：0待处理、1进行中、2已完成、3已取消")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("截止时间")
    private Date deadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("完成时间")
    private Date completedAt;

    public void start() {
        if (!Integer.valueOf(PENDING).equals(status)) {
            throw new IllegalStateException("只有待处理的评标任务才能开始");
        }
        status = IN_PROGRESS;
    }

    public void complete(Date now) {
        if (!Integer.valueOf(IN_PROGRESS).equals(status)) {
            throw new IllegalStateException("只有进行中的评标任务才能完成");
        }
        status = COMPLETED;
        completedAt = now;
    }
}
