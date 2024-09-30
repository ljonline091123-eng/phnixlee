package com.zhaocai.business.receipt.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 材料对账单对象 tb_receipt_reconciliation
 *
 * @author zhangxu
 * @date 2024-09-06
 */
@Getter
@Setter
@TableName(value = "tb_receipt_reconciliation")
@ApiModel(value = "ReceiptReconciliation对象", description = "材料对账单对象")
public class ReceiptReconciliation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 材料对账单id
     */
    @ApiModelProperty("材料对账单id")
    private Long id;

    /**
     * 材料对账单第三方id
     */
    @ApiModelProperty("材料对账单第三方id")
    private String thirdId;

    /**
     * 单据状态
     */
    @ApiModelProperty("单据状态")
    private String orderStatus;

    /**
     * 归属本级组织id
     */
    @ApiModelProperty("归属本级组织id")
    private Long orgId;

    /**
     * 归属本级组织名称
     */
    @ApiModelProperty("归属本级组织名称")
    private String orgName;

    /**
     * 乙方对账人
     */
    @ApiModelProperty("乙方对账人")
    private String planBReconcilerName;

    /**
     * 单据状态
     */
    @ApiModelProperty("单据状态")
    private String procStatus;

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
     * 对账单编码
     */
    @ApiModelProperty("对账单编码")
    private String reconciliationCode;

    /**
     * 对账日期
     */
    @ApiModelProperty("对账日期")
    private String reconciliationDate;

    /**
     * 对账周期-结束时间
     */
    @ApiModelProperty("对账周期-结束时间")
    private String reconciliationEndDate;

    /**
     * 对账周期-开始时间
     */
    @ApiModelProperty("对账周期-开始时间")
    private String reconciliationStartDate;

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
     * 附件业务id
     */
    @ApiModelProperty("附件业务id")
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
     * 第三方创建人
     */
    @ApiModelProperty("第三方创建人")
    private String thirdCreateBy;

    /**
     * 第三方创建时间
     */
    @ApiModelProperty("第三方创建时间")
    private String thirdCreateTime;

    /**
     * 供应商状态(0未发送、1已发送、2已签收、3已确认)
     */
    @ApiModelProperty("供应商状态(0未发送、1已发送、2已签收、3已确认)")
    private String supplierStatus;
}
