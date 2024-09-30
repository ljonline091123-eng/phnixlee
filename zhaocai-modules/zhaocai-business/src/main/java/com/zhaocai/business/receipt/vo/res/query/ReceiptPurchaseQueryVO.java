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
public class ReceiptPurchaseQueryVO extends PageRecive {

    @ApiModelProperty(value = "采购订单名称")
    private String orderName;


    @ApiModelProperty(value = "供应商id")
    private String supplierId;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;
}
