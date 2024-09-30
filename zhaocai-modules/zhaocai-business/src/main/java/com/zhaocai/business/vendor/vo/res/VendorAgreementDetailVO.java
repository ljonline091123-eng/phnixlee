package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.agreement.vo.res.AgreementMaterialsListVO;
import com.zhaocai.business.agreement.vo.res.AgreementPaymentItemVO;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 供应商合同明细
 *
 * @author chenming
 * @date 2024-06-22
 */
@Data
@Builder
public class VendorAgreementDetailVO extends AdviceObject {

    @ApiModelProperty(value = "合同基本信息")
    private VendorAgreementVO agreement;

    @ApiModelProperty(value = "合同款项信息")
    private AgreementPaymentItemVO agreementPaymentItem;

    @ApiModelProperty(value = "物质清单")
    private List<AgreementMaterialsListVO> materialsList;
}
