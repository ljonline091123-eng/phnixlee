package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettlement;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettlementVO", description = "推送结算单VO")
public class ReceiptContractSettlementVO extends ReceiptContractSettlement {

    @ApiModelProperty(value =  "计日工明细")
    private List<ReceiptContractSettleDatallVO> contractSettleDatallerList;

    @ApiModelProperty(value =  "状态名称")
    private String stateName;

    @ApiModelProperty(value =  "押金、保证金信息明细")
    private List<ReceiptContractSettleDepositVO> contractSettleDepositList;

    @ApiModelProperty(value =  "供应商评价明细")
    private List<ReceiptContractSettleEvaluateVO> contractSettleEvaluateList;

    @ApiModelProperty(value =  "清单结算明细")
    private List<ReceiptContractSettleListVO> contractSettleLists;

    @ApiModelProperty(value =  "机械台班明细")
    private List<ReceiptContractSettleMechanicalVO> contractSettleMechanicalTableList;

    @ApiModelProperty(value =  "其他款项结算明细")
    private List<ReceiptContractSettleOtherPaymentVO> contractSettleOtherPaymentList;

    @ApiModelProperty(value =  "零星包工明细")
    private List<ReceiptContractSettleSporadicWorkVO> contractSettleSporadicWorkList;
}
