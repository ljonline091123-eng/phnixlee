package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedger;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptMaterialsTurnLedgerVO", description = "推送租赁周材台账VO")
public class ReceiptMaterialsTurnLedgerVO extends ReceiptMaterialsTurnLedger {

    @ApiModelProperty(value =  "租赁周材台账明细")
    private List<ReceiptMaterialsTurnLedgerDtlVO> dtlList;

    @ApiModelProperty(value =  "状态名称")
    private String stateName;
}
