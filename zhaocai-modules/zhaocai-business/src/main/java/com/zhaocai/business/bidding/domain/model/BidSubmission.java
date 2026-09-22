package com.zhaocai.business.bidding.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 投标提交聚合根。
 *
 * 一个供应商针对一个招标公告只能有一个投标提交，报价调整通过投标版本表达，
 * 不再使用父子投标单复制业务主对象。
 */
@Getter
@Setter
@ApiModel("投标提交")
@TableName("tb_bid_submission")
public class BidSubmission extends BaseEntity {
    private static final long serialVersionUID = 1L;

    public static final int DRAFT = 0;
    public static final int SUBMITTED = 1;
    public static final int WITHDRAWN = 2;
    public static final int REJECTED = 3;

    @ApiModelProperty("招标公告ID")
    private Long noticeId;

    @ApiModelProperty("采购方案ID")
    private Long schemeId;

    @ApiModelProperty("供应商ID")
    private Long vendorId;

    @ApiModelProperty("当前生效投标版本ID")
    private Long currentVersionId;

    @ApiModelProperty("状态：0草稿、1已提交、2已撤回、3已拒绝")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submittedAt;

    public void submit(Long versionId, Date now) {
        if (versionId == null) {
            throw new IllegalArgumentException("投标版本不能为空");
        }
        if (Integer.valueOf(WITHDRAWN).equals(status) || Integer.valueOf(REJECTED).equals(status)) {
            throw new IllegalStateException("已撤回或已拒绝的投标不能再次提交");
        }
        currentVersionId = versionId;
        status = SUBMITTED;
        submittedAt = now;
    }

    public void withdraw() {
        if (!Integer.valueOf(SUBMITTED).equals(status)) {
            throw new IllegalStateException("只有已提交的投标才能撤回");
        }
        status = WITHDRAWN;
    }
}
