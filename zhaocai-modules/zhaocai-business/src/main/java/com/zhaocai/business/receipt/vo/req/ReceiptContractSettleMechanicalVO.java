package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleMechanical;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 机械台班明细
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettleMechanicalVO", description = "推送结算单VO")
public class ReceiptContractSettleMechanicalVO extends ReceiptContractSettleMechanical {
}
