package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 发货单对象 tb_receipt_purchase_invoice
 *
 * @author cff
 * @date 2024-09-13
 */
@Data
@TableName(value = "tb_receipt_purchase_invoice")
@ApiModel(value = "ReceiptPurchaseInvoice对象", description = "发货单对象")
public class ReceiptPurchaseInvoice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 发货单名称
     */
    @ApiModelProperty("发货单名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String brandMaterialName;

    /**
     * 本次发货单金额
     */
    @ApiModelProperty("本次发货单金额")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal shipmentOrderMoney;

    /**
     * 发货单编号
     */
    @ApiModelProperty("发货单编号")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String invoiceCode;

    /**
     * 编码序号
     */
    @ApiModelProperty("编码序号")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String codeSerialNumber;

    /**
     * 发货单日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty("发货单日期,yyyy-MM-dd")
    private Date invoiceDate;

    /**
     * 预计到货日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty("预计到货日期,yyyy-MM-dd")
    private Date expectedDeliveryDate;

    /**
     * 采购订单id
     */
    @ApiModelProperty("采购订单id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long receiptPurchaseId;

    /**
     * 采购订单名称
     */
    @ApiModelProperty("采购订单名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orderName;

    /**
     * 采购订单编码
     */
    @ApiModelProperty("采购订单编码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orderCode;

    /**
     * 采购日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty("采购日期,yyyy-MM-dd")
    private Date orderDate;

    /**
     * 计划进场时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty("计划进场时间,yyyy-MM-dd")
    private Date planEntryTime;

    /**
     *
     */
    @ApiModelProperty("")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createDept;

    /**
     * 采购金额
     */
    @ApiModelProperty("采购金额")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal purchaseMoney;

    /**
     * 提货验收人名称
     */
    @ApiModelProperty("提货验收人名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String acceptanceUserName;

    /**
     * 验收人联系方式
     */
    @ApiModelProperty("验收人联系方式")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String acceptancePhone;

    /**
     * 合同编码
     */
    @ApiModelProperty("合同编码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String conCode;

    /**
     * 合同名称
     */
    @ApiModelProperty("合同名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String conName;

    /**
     * 最小核算项目名称
     */
    @ApiModelProperty("最小核算项目名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String projectName;

    /**
     * 归属本级组织id
     */
    @ApiModelProperty("归属本级组织id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orgId;

    /**
     * 归属本级组织名称
     */
    @ApiModelProperty("归属本级组织名称")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orgName;

    /**
     * 二维码
     */
    @ApiModelProperty("二维码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orCode;

    /**
     * 供应商id
     */
    @ApiModelProperty("供应商id")
    private String supplierId;

    /**
     * 供应商名称
     */
    @ApiModelProperty("供应商名称")
    private String supplierName;

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
                .append("brandMaterialName", getBrandMaterialName())
                .append("invoiceCode", getInvoiceCode())
                .append("invoiceDate", getInvoiceDate())
                .append("expectedDeliveryDate", getExpectedDeliveryDate())
                .append("receiptPurchaseId", getReceiptPurchaseId())
                .append("orderName", getOrderName())
                .append("orderCode", getOrderCode())
                .append("orderDate", getOrderDate())
                .append("planEntryTime", getPlanEntryTime())
                .append("createDept", getCreateDept())
                .append("purchaseMoney", getPurchaseMoney())
                .append("acceptanceUserName", getAcceptanceUserName())
                .append("acceptancePhone", getAcceptancePhone())
                .append("conCode", getConCode())
                .append("conName", getConName())
                .append("projectName", getProjectName())
                .append("orgId", getOrgId())
                .append("orgName", getOrgName())
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
