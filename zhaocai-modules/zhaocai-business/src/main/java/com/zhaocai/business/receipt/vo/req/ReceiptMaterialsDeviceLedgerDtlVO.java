package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtl;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptMaterialsDeviceLedgerDtlVO", description = "推送设备租赁台账详情VO")
public class ReceiptMaterialsDeviceLedgerDtlVO extends ReceiptMaterialsDeviceLedgerDtl {

    @ApiModelProperty(value =  "设备租赁台账明细")
    private List<ReceiptMaterialsDeviceLedgerDtlDtlVO> dtlDtlList;

}
