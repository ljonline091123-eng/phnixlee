package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedger;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptThirdRequestVO", description = "推送设备租赁台账VO")
public class ReceiptMaterialsDeviceLedgerVO extends ReceiptMaterialsDeviceLedger {

    @ApiModelProperty(value =  "设备租赁台账明细")
    private List<ReceiptMaterialsDeviceLedgerDtlVO> dtlList;

    @ApiModelProperty(value =  "状态名称")
    private String stateName;
}
