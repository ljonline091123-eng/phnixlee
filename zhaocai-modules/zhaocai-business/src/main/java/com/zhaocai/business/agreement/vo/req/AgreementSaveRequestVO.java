package com.zhaocai.business.agreement.vo.req;

import com.zhaocai.business.agreement.domain.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 合同保存请求 VO
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
public class AgreementSaveRequestVO {

    @ApiModelProperty(value = "合同基本信息")
    private Agreement agreement;

    @ApiModelProperty(value = "合同款项信息")
    private AgreementPaymentItem agreementPaymentItem;

    @ApiModelProperty(value = "结算与付款节点信息")
    private List<AgreementPaymentList> agreementPaymentLists;

    @ApiModelProperty(value = "合同签约方信息对象")
    private List<AgreementPartyInfo> agreementPartyInfoLists;

    @ApiModelProperty(value = "合同清单")
    private List<AgreementMaterialsList> agreementMaterialsLists;

    @ApiModelProperty(value = "合同保证金")
    private List<AgreementDeposit> agreementDeposits;

    @ApiModelProperty(value = "合同-计日工对象")
    private List<AgreementDailyWage> agreementDailyWageList;

    @ApiModelProperty(value = "合同-机械台班对象")
    private List<AgreementMachineShift> agreementMachineShifts;

    @ApiModelProperty(value = "合同-甲供设备清单对象")
    private List<AgreementEquipmentSupply> agreementEquipmentSupplies;

    @ApiModelProperty(value = "合同-甲供材料清单对象")
    private List<AgreementMaterialSupply> agreementMaterialSupplies;

    @ApiModelProperty(value = "合同附件编辑标识")
    private String templateEditFlag;

    @ApiModelProperty(value = "易料采购合同id")
    private String marketMaterialContractId;
}
