package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleOtherPayment;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 其他款项结算明细
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettleOtherPaymentVO", description = "推送结算单VO")
public class ReceiptContractSettleOtherPaymentVO extends ReceiptContractSettleOtherPayment {
}
