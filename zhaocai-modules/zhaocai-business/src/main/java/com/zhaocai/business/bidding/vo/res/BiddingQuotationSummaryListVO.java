package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/4 11:51
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingQuotationListVO", description = "投标单信息报价汇总列表VO")
public class BiddingQuotationSummaryListVO extends AdviceObject implements Serializable {
    private static final long serialVersionUID = 7377944452027574414L;

    @ApiModelProperty(value =  "主键id")
    private Long id;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @ApiModelProperty(value =  "报价排名")
    private Integer rank;

    @ApiModelProperty(value =  "二次报价设置（默认0关 1开）")
    private Integer twiceQuot;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;


    @ApiModelProperty(value =  "第一轮报价")
    private  BiddingQuotationListVO  fistQuotation;

    @ApiModelProperty(value =  "最后一轮")
    private BiddingQuotationListVO lastQuotation;

    @ApiModelProperty(value =  "所有轮次数据")
    private List<BiddingQuotationListVO> allQuotation;

}
