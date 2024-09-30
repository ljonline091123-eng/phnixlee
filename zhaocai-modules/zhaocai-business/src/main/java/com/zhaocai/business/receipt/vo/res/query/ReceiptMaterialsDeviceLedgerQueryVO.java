package com.zhaocai.business.receipt.vo.res.query;

import com.zhaocai.common.core.annotation.Excel;
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
public class ReceiptMaterialsDeviceLedgerQueryVO extends PageRecive {

    @ApiModelProperty(value = "设备台账编码")
    private String ledgerCode;

    @ApiModelProperty(value = "供应商id")
    private String supplierId;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;
}
