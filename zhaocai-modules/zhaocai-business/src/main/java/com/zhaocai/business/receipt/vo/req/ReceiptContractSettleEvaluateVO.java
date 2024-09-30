package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleEvaluate;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 供应商评价
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettleEvaluateVO", description = "推送结算单VO")
public class ReceiptContractSettleEvaluateVO extends ReceiptContractSettleEvaluate {
}
