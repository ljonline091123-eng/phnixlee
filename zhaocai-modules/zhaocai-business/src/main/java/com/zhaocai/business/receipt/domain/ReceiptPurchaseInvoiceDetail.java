package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 发货单详情对象 tb_receipt_purchase_invoice_detail
 *
 * @author cff
 * @date 2024-09-13
 */
@Data
@TableName(value = "tb_receipt_purchase_invoice_detail")
@ApiModel(value = "ReceiptPurchaseInvoiceDetail对象", description = "发货单详情对象")
public class ReceiptPurchaseInvoiceDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 采购订单明细id
     */
    @ApiModelProperty("采购订单明细id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String detailId;

    /**
     * 品牌
     */
    @ApiModelProperty("品牌")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String brand;

    /**
     * 本次发货量
     */
    @ApiModelProperty("本次发货量")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal shipmentQuantity;

    /**
     * 可下订单数量
     */
    @ApiModelProperty("可下订单数量")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long canOrderNumber;

    /**
     * 支出合同主键
     */
    @ApiModelProperty("支出合同主键")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String conId;

    /**
     * 支出合同清单主键
     */
    @ApiModelProperty("支出合同清单主键")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String contractListId;

    /**
     * 合同数量
     */
    @ApiModelProperty("合同数量")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long contractNumber;

    /**
     * 是否全部入库
     */
    @ApiModelProperty("是否全部入库")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String isAllStorage;

    /**
     * 分类编码
     */
    @ApiModelProperty("分类编码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String materialClassCode;

    /**
     * 物资编码
     */
    @ApiModelProperty("物资编码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String materialCode;

    /**
     * 物资名称
     */
    @ApiModelProperty("物资名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String materialName;

    /**
     * 成本子目唯一id
     */
    @ApiModelProperty("成本子目唯一id")
    private String subjectDtlUniqueId;

    /**
     * 已入库数量
     */
    @ApiModelProperty("已入库数量")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long materialStorageNumber;

    /**
     * 计量单位
     */
    @ApiModelProperty("计量单位")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String measurementUnit;

    /**
     * 计量单位id
     */
    @ApiModelProperty("计量单位id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String measurementUnitId;

    /**
     * 价格类型
     */
    @ApiModelProperty("价格类型")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String priceType;

    /**
     * 本次采购数量
     */
    @ApiModelProperty("本次采购数量")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long purchaseNumber;

    /**
     * 规格型号
     */
    @ApiModelProperty("规格型号")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String specificationModel;

    /**
     * 税率
     */
    @ApiModelProperty("税率")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal taxRate;

    /**
     * 含税总价
     */
    @ApiModelProperty("含税总价")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalIncludeTax;

    /**
     * 不含税总价
     */
    @ApiModelProperty("不含税总价")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalNoTax;

    /**
     * 总价税额
     */
    @ApiModelProperty("总价税额")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalTaxMoney;

    /**
     * 含税单价
     */
    @ApiModelProperty("含税单价")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal unitIncludeTax;

    /**
     * 不含税单价
     */
    @ApiModelProperty("不含税单价")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal unitNoTax;

    /**
     * 税额
     */
    @ApiModelProperty("税额")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String taxMoney;

    /**
     * 本次发货金额不含税
     */
    @ApiModelProperty("本次发货金额不含税")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal thisTimeTotalNoTax;

    /**
     * 本次发货金额含税
     */
    @ApiModelProperty("本次发货金额含税")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal thisTimeUnitInclude;

    /**
     * 本次发货金额税额
     */
    @ApiModelProperty("本次发货金额税额")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal thisTimeTaxMoney;

    /**
     * 父级id
     */
    @ApiModelProperty("父级id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String parntId;

    /**
     *
     */
    @ApiModelProperty("")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createDept;

    /**
     * 创建人 id
     */
    @ApiModelProperty("创建人 id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long createId;

    /**
     * 修改人 id
     */
    @ApiModelProperty("修改人 id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long updateId;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @ApiModelProperty("删除标志（0代表存在 2代表删除）")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String delFlag;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("brand", getBrand())
                .append("shipmentQuantity", getShipmentQuantity())
                .append("canOrderNumber", getCanOrderNumber())
                .append("conId", getConId())
                .append("contractListId", getContractListId())
                .append("contractNumber", getContractNumber())
                .append("isAllStorage", getIsAllStorage())
                .append("materialClassCode", getMaterialClassCode())
                .append("materialCode", getMaterialCode())
                .append("materialName", getMaterialName())
                .append("materialStorageNumber", getMaterialStorageNumber())
                .append("measurementUnit", getMeasurementUnit())
                .append("measurementUnitId", getMeasurementUnitId())
                .append("priceType", getPriceType())
                .append("purchaseNumber", getPurchaseNumber())
                .append("specificationModel", getSpecificationModel())
                .append("taxRate", getTaxRate())
                .append("totalIncludeTax", getTotalIncludeTax())
                .append("totalNoTax", getTotalNoTax())
                .append("totalTaxMoney", getTotalTaxMoney())
                .append("unitIncludeTax", getUnitIncludeTax())
                .append("unitNoTax", getUnitNoTax())
                .append("taxMoney", getTaxMoney())
                .append("thisTimeTotalNoTax", getThisTimeTotalNoTax())
                .append("thisTimeUnitInclude", getThisTimeUnitInclude())
                .append("thisTimeTaxMoney", getThisTimeTaxMoney())
                .append("createDept", getCreateDept())
                .append("createBy", getCreateBy())
                .append("createId", getCreateId())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateId", getUpdateId())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .toString();
    }
}
