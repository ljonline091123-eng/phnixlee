package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoice;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptPurchaseInvoiceVO", description = "发货单VO")
public class ReceiptPurchaseInvoiceVO extends ReceiptPurchaseInvoice {

    @ApiModelProperty(value =  "发货单明细")
    private List<ReceiptPurchaseInvoiceDetailVO> dtlRespList;
}
