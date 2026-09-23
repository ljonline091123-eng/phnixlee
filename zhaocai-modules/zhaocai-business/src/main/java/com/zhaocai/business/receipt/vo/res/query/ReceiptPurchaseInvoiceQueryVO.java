package com.zhaocai.business.receipt.vo.res.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 列表查询
 *
 * @author cff
 * @date 2024-09-09
 */
@Data
public class ReceiptPurchaseInvoiceQueryVO extends PageRecive {

    @ApiModelProperty(value = "发货单编号")
    private String invoiceCode;

    @ApiModelProperty(value = "供应商id")
    private String supplierId;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;
}
