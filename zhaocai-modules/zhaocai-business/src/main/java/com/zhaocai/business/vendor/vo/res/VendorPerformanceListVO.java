package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.vendor.domain.VendorPerformanceEvaluation;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 供应商履约评价
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorPerformanceListVO {

    @ApiModelProperty(value = "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "合作单位")
    private String cooperator;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "优")
    private BigDecimal excellentCount;

    @ApiModelProperty(value = "良")
    private BigDecimal goodCount;

    @ApiModelProperty(value = "合格")
    private BigDecimal qualifiedCount;

    @ApiModelProperty(value = "差")
    private BigDecimal badCount;

    public VendorPerformanceListVO(VendorPerformanceEvaluation evaluation) {
        this.vendorName = evaluation.getPartyBName();
        this.cooperator = evaluation.getPartyAName();
        this.agreementName = evaluation.getAgreementName();
        this.excellentCount = evaluation.getExcellentNum();
        this.goodCount = evaluation.getGoodNum();
        this.qualifiedCount = evaluation.getQualifiedNum();
        this.badCount = evaluation.getBadNum();
    }
}
