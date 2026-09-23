package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleList;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 清单结算明细
 * @author cff
 */
@Data
@ApiModel(value = "ReceiptContractSettleListVO", description = "推送结算单VO")
public class ReceiptContractSettleListVO extends ReceiptContractSettleList {
}
