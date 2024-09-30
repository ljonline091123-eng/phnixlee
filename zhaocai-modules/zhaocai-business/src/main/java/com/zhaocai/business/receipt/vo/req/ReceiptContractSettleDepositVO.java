package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleDeposit;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 押金、保证金信息明细
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettleDepositVO", description = "推送结算单VO")
public class ReceiptContractSettleDepositVO extends ReceiptContractSettleDeposit {
}
