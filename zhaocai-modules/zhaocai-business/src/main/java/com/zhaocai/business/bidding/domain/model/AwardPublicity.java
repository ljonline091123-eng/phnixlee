package com.zhaocai.business.bidding.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** 中标公示，独立管理公示期和发布状态。 */
@Getter
@Setter
@ApiModel("中标公示")
@TableName("tb_award_publicity")
public class AwardPublicity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("定标决策ID")
    private Long decisionId;

    @ApiModelProperty("公示标题")
    private String title;

    @ApiModelProperty("公示内容")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("公示开始时间")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("公示结束时间")
    private Date endTime;

    @ApiModelProperty("状态：0草稿、1公示中、2已结束、3已撤销")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("发布时间")
    private Date publishedAt;

    public void publish(Date now) {
        if (startTime == null || endTime == null || !endTime.after(startTime)) {
            throw new IllegalStateException("公示结束时间必须晚于开始时间");
        }
        status = 1;
        publishedAt = now;
    }
}
