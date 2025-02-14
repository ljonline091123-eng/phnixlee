package com.zhaocai.business.agreement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class AgreementUnderlingMaterialsVO {

    private Long id;

    @ApiModelProperty(value = "合同 id")
    private Long agreementId;

    @ApiModelProperty(value = "物料清单唯一 id")
    private String materialsUniqueId;

    @ApiModelProperty(value = "物料编码")
    private String materialsCode;

    @ApiModelProperty(value = "物料名称")
    private String materialsName;

    @ApiModelProperty(value = "规格型号")
    private String specification;

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "计量规则")
    private String measurementRules;

    @ApiModelProperty(value = "基本工作内容")
    private String workContent;

    @ApiModelProperty(value = "成本科目档案 id")
    private String costAccountId;

    @ApiModelProperty(value = "成本科目编码")
    private String costAccountCode;

    @ApiModelProperty(value = "成本科目名称")
    private String costAccountName;

    @ApiModelProperty(value = "数量")
    private BigDecimal count;

    @ApiModelProperty(value =  "含税单价(元)")
    private BigDecimal taxUnitPrice;

    @ApiModelProperty(value =  "不含税单价(元)")
    private BigDecimal notTaxUnitPrice;

    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    @ApiModelProperty(value =  "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value =  "税率编码")
    private String taxRateCode;

    @ApiModelProperty(value =  "税率编码名称")
    private String taxRateName;

    @ApiModelProperty(value = "税额")
    private BigDecimal taxAmount;

    @ApiModelProperty(value =  "发票类型")
    private Integer billType;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @ApiModelProperty(value = "价款类型")
    private String paymentType;

    @ApiModelProperty(value = "工作量")
    private String workload;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "浮动价")
    private BigDecimal floatingPrice;

    @ApiModelProperty(value = "卸费")
    private BigDecimal unloadingFee;

    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    @ApiModelProperty(value = "租赁方式")
    private String rentMode;

    @ApiModelProperty(value = "计租单位")
    private String rentalUnit;

    @ApiModelProperty(value = "租赁时间")
    private BigDecimal rentTime;

    @ApiModelProperty(value = "租赁数量")
    private BigDecimal rentQuantity;

    @ApiModelProperty(value = "总价(含税)")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "浮动率")
    private BigDecimal floatingRate;
}
