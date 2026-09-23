package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/22 14:37
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingResultDetailVO", description = "投标结果详情VO")
public class BiddingResultDetailVO extends AdviceObject implements Serializable {
    private static final long serialVersionUID = -4362119799459435338L;

    @ApiModelProperty(value = "主键id")
    private Long id;

    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "投标单信息id")
    private Long biddingInfoId;

    @ApiModelProperty(value = "中标候选人")
    private String candidate;

    @ApiModelProperty(value = "综合分")
    private BigDecimal totalScore;

    @ApiModelProperty(value = "综合排名")
    private Integer rank;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-起")
    private Date publicityStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-止")
    private Date publicityEndTime;

    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private Integer bidResult;

    @ApiModelProperty(value = "是否发送通知书（0未发送 1已发送）")
    private Integer sendNotified;

    @ApiModelProperty(value = "中标通知书内容")
    private String notifiContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "中标通知发布时间")
    private Date notifiTime;

    @ApiModelProperty(value = "联系人")
    private String contact;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxPrice")
    @ApiModelProperty(value =  "含税总价(元)（千分位）")
    private String taxPricePattern;

    @MoneyFormat(filedName = "notTaxPrice")
    @ApiModelProperty(value =  "不含税总价(元)（千分位）")
    private String notTaxPricePattern;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "投标时间")
    private Date bidTime;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

}
