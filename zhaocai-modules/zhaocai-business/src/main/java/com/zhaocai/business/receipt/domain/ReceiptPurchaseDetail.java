package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 采购订单详情对象 tb_receipt_purchase_detail
 *
 * @author CFF
 * @date 2024-09-06
 */
@Data
@TableName(value = "tb_receipt_purchase_detail")
@ApiModel(value = "ReceiptPurchaseDetail对象", description = "采购订单详情对象")
public class ReceiptPurchaseDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 品牌
     */
    @ApiModelProperty("品牌")
    private String brand;

    /**
     * 本次发货量
     */
    @ApiModelProperty("本次发货量")
    private Long shipmentQuantity;

    /**
     * 可下订单数量
     */
    @ApiModelProperty("可下订单数量")
    private Long canOrderNumber;

    /**
     * 剩余订单数量
     */
    @ApiModelProperty("剩余订单数量")
    private Long residueOrderNumber;

    /**
     * 支出合同主键
     */
    @ApiModelProperty("支出合同主键")
    private String conId;

    /**
     * 支出合同清单主键
     */
    @ApiModelProperty("支出合同清单主键")
    private String contractListId;

    /**
     * 合同数量
     */
    @ApiModelProperty("合同数量")
    private Long contractNumber;

    /**
     * 是否全部入库
     */
    @ApiModelProperty("是否全部入库")
    private String isAllStorage;

    /**
     * 分类编码
     */
    @ApiModelProperty("分类编码")
    private String materialClassCode;

    /**
     * 物资编码
     */
    @ApiModelProperty("物资编码")
    private String materialCode;

    /**
     * 成本子目唯一id
     */
    @ApiModelProperty("成本子目唯一id")
    private String subjectDtlUniqueId;

    /**
     * 物资名称
     */
    @ApiModelProperty("物资名称")
    private String materialName;

    /**
     * 已入库数量
     */
    @ApiModelProperty("已入库数量")
    private Long materialStorageNumber;

    /**
     * 计量单位
     */
    @ApiModelProperty("计量单位")
    private String measurementUnit;

    /**
     * 计量单位id
     */
    @ApiModelProperty("计量单位id")
    private String measurementUnitId;

    /**
     * 价格类型
     */
    @ApiModelProperty("价格类型")
    private String priceType;

    /**
     * 本次采购数量
     */
    @ApiModelProperty("本次采购数量")
    private Long purchaseNumber;

    /**
     * 规格型号
     */
    @ApiModelProperty("规格型号")
    private String specificationModel;

    /**
     * 税率
     */
    @ApiModelProperty("税率")
    private BigDecimal taxRate;

    /**
     * 含税总价
     */
    @ApiModelProperty("含税总价")
    private BigDecimal totalIncludeTax;

    /**
     * 不含税总价
     */
    @ApiModelProperty("不含税总价")
    private BigDecimal totalNoTax;

    /**
     * 总价税额
     */
    @ApiModelProperty("总价税额")
    private BigDecimal totalTaxMoney;

    /**
     * 含税单价
     */
    @ApiModelProperty("含税单价")
    private BigDecimal unitIncludeTax;

    /**
     * 不含税单价
     */
    @ApiModelProperty("不含税单价")
    private BigDecimal unitNoTax;

    /**
     * 父级id
     */
    @ApiModelProperty("父级id")
    private String parntId;

    /**
     * $column.columnComment
     */
    @ApiModelProperty("$column.columnComment")
    private String createDept;

    /**
     * 创建人 id
     */
    @ApiModelProperty("创建人 id")
    private Long createId;

    /**
     * 修改人 id
     */
    @ApiModelProperty("修改人 id")
    private Long updateId;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @ApiModelProperty("删除标志（0代表存在 2代表删除）")
    private String delFlag;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("brand", getBrand())
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
                .append("parntId", getParntId())
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
