package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 采购订单对象 tb_receipt_purchase
 *
 * @author CFF
 * @date 2024-09-06
 */
@Data
@TableName(value = "tb_receipt_purchase")
@ApiModel(value = "ReceiptPurchase对象", description = "采购订单对象")
public class ReceiptPurchase extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 采购订单id
     */
    @ApiModelProperty("采购订单id")
    private String thirdId;

    /**
     * 0生成发货单 1已生成
     */
    @ApiModelProperty("0生成发货单 1已生成")
    private String state;

    /**
     * 验收人联系方式
     */
    @ApiModelProperty("验收人联系方式")
    private String acceptancePhone;

    /**
     * 验收人id
     */
    @ApiModelProperty("验收人id")
    private String acceptanceUserId;

    /**
     * 提货验收人名称
     */
    @ApiModelProperty("提货验收人名称")
    private String acceptanceUserName;

    /**
     *
     */
    @ApiModelProperty("")
    private String attachBusinessId;

    /**
     * 支出合同编码
     */
    @ApiModelProperty("支出合同编码")
    private String conCode;

    /**
     * 支出合同id
     */
    @ApiModelProperty("支出合同id")
    private String conId;

    /**
     * 支出合同名称
     */
    @ApiModelProperty("支出合同名称")
    private String conName;

    /**
     * 提货方式(字典表)
     */
    @ApiModelProperty("提货方式(字典表)")
    private String deliveryMethod;

    /**
     * 详细地址
     */
    @ApiModelProperty("详细地址")
    private String detailAddress;

    /**
     * 采购订单编码
     */
    @ApiModelProperty("采购订单编码")
    private String orderCode;

    /**
     * 采购日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("采购日期,yyyy-MM-dd HH:mm:ss")
    private Date orderDate;

    /**
     * 采购订单名称
     */
    @ApiModelProperty("采购订单名称")
    private String orderName;

    /**
     * 订单状态
     */
    @ApiModelProperty("订单状态")
    private String orderStatus;

    /**
     * 归属本级组织id
     */
    @ApiModelProperty("归属本级组织id")
    private String orgId;

    /**
     * 归属本级组织名称
     */
    @ApiModelProperty("归属本级组织名称")
    private String orgName;

    /**
     * 计划进场时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("计划进场时间,yyyy-MM-dd HH:mm:ss")
    private Date planEntryTime;

    /**
     * 审批状态
     */
    @ApiModelProperty("审批状态")
    private String procStatus;

    /**
     *
     */
    @ApiModelProperty("")
    private String processId;

    /**
     * 最小核算项目编码
     */
    @ApiModelProperty("最小核算项目编码")
    private String projectCode;

    /**
     * 最小核算项目id
     */
    @ApiModelProperty("最小核算项目id")
    private String projectId;

    /**
     * 最小核算项目名称
     */
    @ApiModelProperty("最小核算项目名称")
    private String projectName;

    /**
     * 采购金额
     */
    @ApiModelProperty("采购金额")
    private BigDecimal purchaseMoney;

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
     *
     */
    @ApiModelProperty("")
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
                .append("thirdId", getThirdId())
                .append("acceptancePhone", getAcceptancePhone())
                .append("acceptanceUserId", getAcceptanceUserId())
                .append("acceptanceUserName", getAcceptanceUserName())
                .append("attachBusinessId", getAttachBusinessId())
                .append("conCode", getConCode())
                .append("conId", getConId())
                .append("conName", getConName())
                .append("deliveryMethod", getDeliveryMethod())
                .append("detailAddress", getDetailAddress())
                .append("orderCode", getOrderCode())
                .append("orderDate", getOrderDate())
                .append("orderName", getOrderName())
                .append("orderStatus", getOrderStatus())
                .append("orgId", getOrgId())
                .append("orgName", getOrgName())
                .append("planEntryTime", getPlanEntryTime())
                .append("procStatus", getProcStatus())
                .append("processId", getProcessId())
                .append("projectCode", getProjectCode())
                .append("projectId", getProjectId())
                .append("projectName", getProjectName())
                .append("purchaseMoney", getPurchaseMoney())
                .append("remark", getRemark())
                .append("supplierId", getSupplierId())
                .append("supplierName", getSupplierName())
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
