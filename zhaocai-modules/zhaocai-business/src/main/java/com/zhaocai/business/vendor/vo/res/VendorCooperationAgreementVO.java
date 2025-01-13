package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 供应商合作
 *
 * @author chenming
 * @date 2024-06-29
 */
@Data
@NoArgsConstructor
public class VendorCooperationAgreementVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "合作单位名称")
    private String cooperativePartnerName;

    @ApiModelProperty(value = "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    @ApiModelProperty(value = "合同支出业务分类")
    private Integer expenditureBusinessType;

    @ApiModelProperty(value = "合同金额(元)")
    private BigDecimal totalAmountIncTax;

    @ApiModelProperty(value = "已结算金额(元)")
    private BigDecimal settledAmount;

    @ApiModelProperty(value = "已付款金额(元)")
    private BigDecimal paidAmount ;

    @ApiModelProperty(value = "未付款金额(元)")
    private BigDecimal unpaidAmount;

    @ApiModelProperty(value = "优-数量")
    private BigDecimal excellentNum;

    @ApiModelProperty(value = "良-数量")
    private BigDecimal goodNum;

    @ApiModelProperty(value = "合格-数量")
    private BigDecimal qualifiedNum;

    @ApiModelProperty(value = "差-数量")
    private BigDecimal badNum;

    @ApiModelProperty(value = "子集")
    private List<VendorCooperationAgreementVO> children;

    // 父引用
    @Ignore
    private VendorCooperationAgreementVO parent;

    public void addChildren(VendorCooperationAgreementVO cooperationAgreement) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }

        this.children.add(cooperationAgreement);
    }

    @MoneyFormat(filedName = "totalAmountIncTax",scale = 2)
    @ApiModelProperty(value = "合同金额(元)")
    private String totalAmountIncTaxText;

    @MoneyFormat(filedName = "settledAmount",scale = 2)
    @ApiModelProperty(value = "已结算金额(元)")
    private String settledAmountText;

    @MoneyFormat(filedName = "paidAmount",scale = 2)
    @ApiModelProperty(value = "已付款金额(元)")
    private String paidAmountText;

    @MoneyFormat(filedName = "unpaidAmount",scale = 2)
    @ApiModelProperty(value = "未付款金额(元)")
    private String unpaidAmountText;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "合同签订日期")
    private Date agreementSignDate;

    private BigDecimal childrenNum;
}
