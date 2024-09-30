package com.zhaocai.business.receipt.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author zhangxu
 * @date 2024-09-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReceiptReconciliationDetailVO", description = "材料对账单详情VO")
public class ReceiptReconciliationDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 入库日期 */
    @ApiModelProperty(value = "入库日期")
    private String actualEntryDate;

    /** 支出合同清单主键 */
    @ApiModelProperty(value = "支出合同清单主键")
    private String conDtlId;

    /** docId */
    @ApiModelProperty(value = "docId")
    private String docId;

    /** 物资类编码 */
    @ApiModelProperty(value = "物资类编码")
    private String materialClassCode;

    /** 物资编码 */
    @ApiModelProperty(value = "物资编码")
    private String materialCode;

    /** 物资名称 */
    @ApiModelProperty(value = "物资名称")
    private String materialName;

    /** 入库单编码 */
    @ApiModelProperty(value = "入库单编码")
    private String materialsStorageCode;

    /** 入库单主键 */
    @ApiModelProperty(value = "入库单主键")
    private String materialsStorageDtlId;

    /** 入库单主键 */
    @ApiModelProperty(value = "入库单主键")
    private String materialsStorageId;

    /** 计量单位 */
    @ApiModelProperty(value = "计量单位")
    private String measurementUnit;

    /** 价格类型 */
    @ApiModelProperty(value = "价格类型")
    private String priceType;

    /** 本次对账金额(含税) */
    @ApiModelProperty(value = "本次对账金额(含税)")
    private BigDecimal reconciliationIncludeTax;

    /** 本次对账金额(不含税) */
    @ApiModelProperty(value = "本次对账金额(不含税)")
    private BigDecimal reconciliationNoTax;

    /** 本次对账金额(税额) */
    @ApiModelProperty(value = "本次对账金额(税额)")
    private BigDecimal reconciliationTaxMoney;

    /** 规格型号 */
    @ApiModelProperty(value = "规格型号")
    private String specificationModel;

    /** 入库数量 */
    @ApiModelProperty(value = "入库数量")
    private BigDecimal storageNumber;

    /** 税率 */
    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    /** 含税单价 */
    @ApiModelProperty(value = "含税单价")
    private BigDecimal unitIncludeTax;

    /** 不含税单价 */
    @ApiModelProperty(value = "不含税单价")
    private BigDecimal unitNoTax;

    /** 单价税额 */
    @ApiModelProperty(value = "单价税额")
    private BigDecimal unitTaxMoney;
}
