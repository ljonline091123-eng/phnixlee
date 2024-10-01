package com.zhaocai.business.bidding.vo.res;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * @date 2024/5/31 14:20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingInfoListVO", description = "投标单信息列表VO")
public class BiddingInfoListVO extends AdviceObject implements Serializable {
    private static final long serialVersionUID = -8601634506593501074L;

    @ApiModelProperty(value =  "投标单id")
    private Long id;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "是否收取保证金")
    private Integer collectDeposit;

    @ApiModelProperty(value =  "投标状态")
    private Integer biddingStatus;

    @ApiModelProperty(value =  "投标状态（文本）")
    private String biddingStatusText;

    @ApiModelProperty(value = "创建时间（投标时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;

    @ApiModelProperty(value =  "操作人ip")
    private String ipAddress;

    @ApiModelProperty(value =  "是否开标（0待开标 1已开标）")
    private String biddingOpenStatus;

    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxPrice")
    @ApiModelProperty(value =  "含税总价(元)（千分位）")
    private String taxPricePattern;

    @MoneyFormat(filedName = "notTaxPrice")
    @ApiModelProperty(value =  "不含税总价(元)（千分位）")
    private String notTaxPricePattern;

}
