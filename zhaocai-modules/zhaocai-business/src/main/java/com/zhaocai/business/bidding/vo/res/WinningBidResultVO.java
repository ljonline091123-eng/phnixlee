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
 * @date 2024/6/20 10:14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "WinningBidResultVO", description = "中标结果数据列表VO")
public class WinningBidResultVO extends AdviceObject implements Serializable {
    private static final long serialVersionUID = -7944170007390296054L;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "中标候选人名次")
    private String candidate;

    @ApiModelProperty(value = "联系人")
    private String contact;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private Integer bidResult;

    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private String bidResultText;

    @ApiModelProperty(value = "是否发送通知书（0未发送 1已发送）")
    private Integer sendNotified;

    /** 投标单信息 */
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "中标通知发布时间")
    private Date notifiTime;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

}
