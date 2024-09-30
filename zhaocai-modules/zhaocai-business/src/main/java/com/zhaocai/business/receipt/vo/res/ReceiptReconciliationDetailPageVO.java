package com.zhaocai.business.receipt.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author zhangxu
 * @date 2024-09-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReceiptReconciliationUpVO", description = "材料对账单详情查询VO")
public class ReceiptReconciliationDetailPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 对账单id
     */
    @ApiModelProperty(value = "对账单id")
    private Long id;

    /**
     * 对账单编码
     */
    @ApiModelProperty(value = "对账单编码")
    private String reconciliationCode;

    /**
     * 支出合同名称
     */
    @ApiModelProperty(value = "支出合同名称")
    private String conName;

    /**
     * 对账日期
     */
    @ApiModelProperty(value = "对账日期")
    private String reconciliationDate;

    /**
     * 对账周期-开始时间
     */
    @ApiModelProperty(value = "对账周期-开始时间")
    private String reconciliationStartDate;

    /**
     * 对账周期-结束时间
     */
    @ApiModelProperty(value = "对账周期-结束时间")
    private String reconciliationEndDate;

    /**
     * 对账单详情列表
     */
    @ApiModelProperty(value = "对账单详情列表")
    private ArrayList reconciliationDetailList;

}
