package com.zhaocai.business.receipt.vo.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtl;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptMaterialsTurnLedgerDtlVO", description = "推送周材租赁台账详情VO")
public class ReceiptMaterialsTurnLedgerDtlVO extends ReceiptMaterialsTurnLedgerDtl {

    @ApiModelProperty(value =  "租赁周材台账明细")
    private List<ReceiptMaterialsTurnLedgerDtlDtlVO> dtlDtlList;


}
