package com.zhaocai.business.receipt.vo.req;

import com.zhaocai.business.receipt.domain.ReceiptPurchase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author CFF
 */
@Data
@ApiModel(value = "ReceiptPurchaseVO", description = "推送采购订单VO")
public class ReceiptPurchaseVO extends ReceiptPurchase {

    @ApiModelProperty(value =  "物资明细")
    private List<ReceiptPurchaseDetailVO> dtlRespList;

    @ApiModelProperty(value =  "状态名称")
    private String stateName;

    @ApiModelProperty(value =  "发货单名称")
    private String brandMaterialName;

    @ApiModelProperty(value =  "发货单编号")
    private String invoiceCode;

    @ApiModelProperty(value =  "编码序号")
    private String codeSerialNumber;

    @ApiModelProperty(value =  "本次发货金额（含税）")
    private String deliveryAmountSum;

}
