package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 投标结果信息对象 tb_bidding_result
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_bidding_result")
public class BiddingResult extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购方案id
     */
    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;

    /**
     * 招标公告id
     */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    /**
     * 投标单信息id
     */
    @ApiModelProperty(value = "投标单信息id")
    private Long biddingInfoId;

    /**
     * 中标候选人（第一、第二、第三）
     */
    @ApiModelProperty(value = "中标候选人")
    private String candidate;

    @ApiModelProperty(value = "确定中标（1：确定中标人 其它：不确定中标人）")
    private Integer sureBid;

    /**
     * 综合分
     */
    @ApiModelProperty(value = "综合分")
    private BigDecimal totalScore;

    /**
     * 综合排名
     */
    @ApiModelProperty(value = "综合排名")
    @TableField("`rank`")
    private Integer rank;

    /**
     * 公示期时间-起
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-起")
    private Date publicityStartTime;

    /**
     * 公示期时间-止
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-止")
    private Date publicityEndTime;

    /**
     * 中标结果（0未中标 1已中标）
     */
    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private Integer bidResult;

    /**
     * 是否发送通知书（0未发送 1已发送）
     */
    @ApiModelProperty(value = "是否发送通知书（0未发送 1已发送）")
    private Integer sendNotified;

    /**
     * 中标通知书内容
     */
    @ApiModelProperty(value = "中标通知书内容")
    private String notifiContent;

    /**
     * 中标通知发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "中标通知发布时间")
    private Date notifiTime;

    /**
     * 联系人
     */
    @ApiModelProperty(value = "联系人")
    private String contact;

    /**
     * 联系电话
     */
    @ApiModelProperty(value = "联系电话")
    private String phone;

}
