package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleSporadicWork;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 零星包工明细
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettleSporadicWorkVO", description = "推送结算单VO")
public class ReceiptContractSettleSporadicWorkVO extends ReceiptContractSettleSporadicWork {
}
