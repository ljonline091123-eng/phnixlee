package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoiceDetail;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptPurchaseInvoiceVO", description = "发货单详情VO")
public class ReceiptPurchaseInvoiceDetailVO extends ReceiptPurchaseInvoiceDetail {

    @ApiModelProperty(value =  "剩余订单数量")
    private Long residueOrderNumber;

}
