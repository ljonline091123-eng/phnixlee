package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.agreement.vo.res.AgreementPartyInfoVO;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 合同详情
 *
 * @author chenming
 * @date 2024-06-21
 */
@Data
@Builder
public class AgreementDetailVO extends AdviceObject {

    @ApiModelProperty(value = "合同基本信息")
    private AgreementVO agreement;

    @ApiModelProperty(value = "合同款项信息")
    private AgreementPaymentItemVO agreementPaymentItem;

    @ApiModelProperty(value = "结算与付款节点信息")
    private List<AgreementPaymentListVO> agreementPaymentLists;

    @ApiModelProperty(value = "合同签约方信息对象")
    private List<AgreementPartyInfoVO> agreementPartyInfoLists;

    @ApiModelProperty(value = "物质清单")
    private List<AgreementMaterialsListVO> materialsList;

    @ApiModelProperty(value = "合同保证金")
    private List<AgreementDepositVO> agreementDeposits;

    @ApiModelProperty(value = "合同-计日工对象")
    private List<AgreementDailyWageVO> agreementDailyWageList;

    @ApiModelProperty(value = "合同-机械台班对象")
    private List<AgreementMachineShiftVO> agreementMachineShifts;

    @ApiModelProperty(value = "合同-甲供设备清单对象")
    private List<AgreementEquipmentSupplyVO> agreementEquipmentSupplies;

    @ApiModelProperty(value = "合同-甲供材料清单对象")
    private List<AgreementMaterialSupplyVO> agreementMaterialSupplies;
}
