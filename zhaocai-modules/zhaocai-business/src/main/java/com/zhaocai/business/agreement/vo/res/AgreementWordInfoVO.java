package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.vendor.domain.Vendor;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同文本 vo
 *
 * @author chenming
 * @date 2024-06-20
 */
@Data
public class AgreementWordInfoVO {

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "项目名称")
    private String belongOrganizationName;

    @ApiModelProperty(value = "合同签订金额(含税)-大写")
    private String totalAmountIncTaxText;

    @ApiModelProperty(value = "合同签订金额(含税)")
    private BigDecimal totalAmountIncTax;

    @ApiModelProperty(value = "合同签订金额(不含税)-大写")
    private String totalAmountExcTaxText;

    @ApiModelProperty(value = "合同签订金额(不含税)")
    private BigDecimal totalAmountExcTax;

    @ApiModelProperty(value = "物质清单")
    private List<VendorBiddingListQuotationListVO> biddingListQuotationList;

    @ApiModelProperty(value = "供应商信息")
    private Vendor vendor;

    @ApiModelProperty(value = "甲方联系人")
    private AgreementContactVO partyAContact;

    @ApiModelProperty(value = "乙方联系人")
    private AgreementContactVO partyBContact;
}
