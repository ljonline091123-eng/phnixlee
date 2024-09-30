package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商合作列表vo
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class VendorCooperationListVO extends AdviceObject {

    @ApiModelProperty(value = "供应商 id")
    private Long id;

    @ApiModelProperty(value = "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "统一社会信用代码")
    private String socialCreditCode;

    @ApiModelProperty(value =  "联系人名称")
    private String contactName;

    @ApiModelProperty(value =  "联系人电话")
    private String contactPhone;

    @ApiModelProperty(value =  "注册资金")
    private BigDecimal registeredCapital;

    @ApiModelProperty(value = "企业所在地")
    private String vendorAddress;

    @ApiModelProperty(value = "供应商合作金额")
    private BigDecimal cooperationAmount;

    @ApiModelProperty(value = "供应商评价(优)")
    private Long excellentNum;

    @ApiModelProperty(value =  "注册资金")
    @MoneyFormat(filedName = "registeredCapital")
    private String registeredCapitalText;

    @ApiModelProperty(value = "供应商合作金额")
    @MoneyFormat(filedName = "cooperationAmount")
    private String cooperationAmountText;

    public Long getExcellentNum() {
        return this.excellentNum == null ? 0 : this.excellentNum;
    }
}
