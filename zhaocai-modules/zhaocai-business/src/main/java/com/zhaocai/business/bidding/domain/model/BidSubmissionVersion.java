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

/** 投标版本，保存首次报价及每次调价形成的不可变业务快照。 */
@Getter
@Setter
@ApiModel("投标版本")
@TableName("tb_bid_submission_version")
public class BidSubmissionVersion extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("投标提交ID")
    private Long submissionId;

    @ApiModelProperty("前一版本ID")
    private Long previousVersionId;

    @ApiModelProperty("版本号，从1开始")
    private Integer versionNo;

    @ApiModelProperty("含税总价")
    private BigDecimal taxPrice;

    @ApiModelProperty("不含税总价")
    private BigDecimal notTaxPrice;

    @ApiModelProperty("投标联系人")
    private String contact;

    @ApiModelProperty("联系电话")
    private String phone;

    @ApiModelProperty("版本状态：0草稿、1已提交、2已作废")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("版本提交时间")
    private Date submittedAt;
}
