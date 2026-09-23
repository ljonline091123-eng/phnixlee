package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商绩业绩列表
 *
 * @author chenming
 * @date 2024/06/11
 */
@Data
public class VendorAgreementListVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "甲方")
    private String partyAName;

    @ApiModelProperty(value = "乙方")
    private String partyBName;

    @ApiModelProperty(value = "项目名称")
    private String belongAccountingItem;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @JsonIgnore
    @ApiModelProperty(value = "合同签订金额(含税)")
    private BigDecimal totalAmountIncTax;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private String createTime;

    @ApiModelProperty(value = "联系人")
    private String contactName;

    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    @ApiModelProperty(value = "合同状态")
    private Integer agreementState;

    @ApiModelProperty(value = "是否可操作")
    private Integer operateFlag;

    @ApiModelProperty(value = "签章状态")
    private String signStateText;

    @ApiModelProperty(value = "合同签订金额(含税)")
    @MoneyFormat(filedName = "totalAmountIncTax",scale = 2)
    private String totalAmountIncTaxText;
}
