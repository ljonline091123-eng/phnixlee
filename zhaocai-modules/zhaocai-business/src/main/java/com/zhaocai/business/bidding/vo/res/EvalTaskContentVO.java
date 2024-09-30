package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/19 13:54
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EvalTaskContentVO", description = "投标供应商及评分情况信息VO")
public class EvalTaskContentVO extends AdviceObject {

    @ApiModelProperty(value =  "投标单id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxPrice")
    @ApiModelProperty(value =  "含税总价（千分位）")
    private String taxPricePattern;

    @MoneyFormat(filedName = "notTaxPrice")
    @ApiModelProperty(value =  "不含税总价（千分位）")
    private String notTaxPricePattern;

    @ApiModelProperty(value =  "投标标书附件")
    private List<AttachmentVO> attachments;

    @ApiModelProperty(value =  "商务评分")
    private BigDecimal busScore;

    @ApiModelProperty(value =  "技术评分")
    private BigDecimal techScore;

    @ApiModelProperty(value =  "是否评标 0未评标 1已评标")
    private Boolean isEval;

}
