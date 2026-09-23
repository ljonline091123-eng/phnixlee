package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptPurchaseDetail;
import io.swagger.annotations.ApiModel;
import lombok.Data;
/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptPurchaseVO", description = "推送采购订单明细VO")
public class ReceiptPurchaseDetailVO extends ReceiptPurchaseDetail {

}
