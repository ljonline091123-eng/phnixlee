package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 结算单单据主表对象 tb_receipt_contract_settlement
 *
 * @author CFF
 * @date 2024-09-07
 */
@Data
@TableName(value = "tb_receipt_contract_settlement")
@ApiModel(value = "ReceiptContractSettlement对象", description = "结算单单据主表对象")
public class ReceiptContractSettlement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 0待确认 1已确认
     */
    @ApiModelProperty("0待确认 1已确认")
    private String state;

    /**
     * 附件id
     */
    @ApiModelProperty("附件id")
    private Long attachBusinessId;

    /**
     * 结算单编码
     */
    @ApiModelProperty("结算单编码")
    private String billCode;

    /**
     * 合同编码
     */
    @ApiModelProperty("合同编码")
    private String conCode;

    /**
     * 合同id
     */
    @ApiModelProperty("合同id")
    private String conId;

    /**
     * 合同名称
     */
    @ApiModelProperty("合同名称")
    private String conName;

    /**
     * 支出业务分类( 0-其他 1-劳务分包 2-专业分包 3-购买材料 4-租赁材料 5-设备租赁（机械）)
     */
    @ApiModelProperty("支出业务分类( 0-其他 1-劳务分包 2-专业分包 3-购买材料 4-租赁材料 5-设备租赁（机械）)")
    private String conType;

    /**
     * 合同唯一id
     */
    @ApiModelProperty("合同唯一id")
    private String conUniqueId;

    /**
     * 本期应扣回预付款
     */
    @ApiModelProperty("本期应扣回预付款")
    private BigDecimal currentImprestDeductAmount;

    /**
     * 本期合同外结算金额（不含税）(元)
     */
    @ApiModelProperty("本期合同外结算金额（不含税）(元)")
    private BigDecimal currentNtaxOutConSettleAmount;

    /**
     * 本期付款义务（不含税）( = 本期结算金额（含税）*当前付款比例（%）)
     */
    @ApiModelProperty("本期付款义务（不含税）( = 本期结算金额（含税）*当前付款比例（%）)")
    private BigDecimal currentNtaxPaymentObligation;

    /**
     * 本期结算金额（不含税）（元）
     */
    @ApiModelProperty("本期结算金额（不含税）（元）")
    private BigDecimal currentNtaxSettleAmount;

    /**
     * 本期合同外结算占合同金额比例（%）（=其中本期合同外结算金额(含税）/合同金额（含税））
     */
    @ApiModelProperty("本期合同外结算占合同金额比例（%）（=其中本期合同外结算金额(含税）/合同金额（含税））")
    private BigDecimal currentOutConSettleRatio;

    /**
     * 当前付款比例（%）
     */
    @ApiModelProperty("当前付款比例（%）")
    private BigDecimal currentPaymentRatio;

    /**
     * 本期结算比例（%）
     */
    @ApiModelProperty("本期结算比例（%）")
    private BigDecimal currentSettleAmountRatio;

    /**
     * 本期合同外结算金额（含税）(元)
     */
    @ApiModelProperty("本期合同外结算金额（含税）(元)")
    private BigDecimal currentTaxOutConSettleAmount;

    /**
     * 本期付款义务（含税）( = 本期结算金额（不含税）*当前付款比例（%）)
     */
    @ApiModelProperty("本期付款义务（含税）( = 本期结算金额（不含税）*当前付款比例（%）)")
    private BigDecimal currentTaxPaymentObligation;

    /**
     * 本期结算金额（含税）（元）
     */
    @ApiModelProperty("本期结算金额（含税）（元）")
    private BigDecimal currentTaxSettleAmount;

    /**
     * 本期末累计合同外结算金额（不含税）(元)
     */
    @ApiModelProperty("本期末累计合同外结算金额（不含税）(元)")
    private BigDecimal currentTotalNtaxOutConSettleAmount;

    /**
     * 本期末累计付款义务（不含税）
     */
    @ApiModelProperty("本期末累计付款义务（不含税）")
    private BigDecimal currentTotalNtaxPaymentObligation;

    /**
     * 本期末累计结算金额（不含税）（元）
     */
    @ApiModelProperty("本期末累计结算金额（不含税）（元）")
    private BigDecimal currentTotalNtaxSettleAmount;

    /**
     * 本期末累计合同外结算占合同金额比例（%）
     */
    @ApiModelProperty("本期末累计合同外结算占合同金额比例（%）")
    private BigDecimal currentTotalOutConSettleRatio;

    /**
     * 本期末累计结算比例（%）
     */
    @ApiModelProperty("本期末累计结算比例（%）")
    private BigDecimal currentTotalSettleAmountRatio;

    /**
     * 本期末累计合同外结算金额（含税）(元)
     */
    @ApiModelProperty("本期末累计合同外结算金额（含税）(元) ")
    private BigDecimal currentTotalTaxOutConSettleAmount;

    /**
     * 本期末累计付款义务（含税）
     */
    @ApiModelProperty("本期末累计付款义务（含税）")
    private BigDecimal currentTotalTaxPaymentObligation;

    /**
     * 本期末累计结算金额（含税）（元）
     */
    @ApiModelProperty("本期末累计结算金额（含税）（元）")
    private BigDecimal currentTotalTaxSettleAmount;

    /**
     * 发票类型（1-专票 2-普票 3-数电票）
     */
    @ApiModelProperty("发票类型（1-专票 2-普票 3-数电票）")
    private String invoiceType;

    /**
     * 最后审批人
     */
    @ApiModelProperty("最后审批人")
    private String lastProcApprover;

    /**
     * 下一审批人
     */
    @ApiModelProperty("下一审批人")
    private String nextProcApprover;

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
     * 合同甲方名称
     */
    @ApiModelProperty("合同甲方名称")
    private String partaName;

    /**
     * 合同乙方名称
     */
    @ApiModelProperty("合同乙方名称")
    private String partbName;

    /**
     * 上期末累计合同外结算金额（不含税）(元)
     */
    @ApiModelProperty("上期末累计合同外结算金额（不含税）(元)")
    private BigDecimal perTotalNtaxOutConSettleAmount;

    /**
     * 上期末累计付款义务（不含税）
     */
    @ApiModelProperty("上期末累计付款义务（不含税）")
    private BigDecimal perTotalNtaxPaymentObligation;

    /**
     * 上期末累计结算金额（不含税）（元）
     */
    @ApiModelProperty("上期末累计结算金额（不含税）（元）")
    private BigDecimal perTotalNtaxSettleAmount;

    /**
     * 上期末累计合同外结算占合同金额比例（%）
     */
    @ApiModelProperty("上期末累计合同外结算占合同金额比例（%）")
    private BigDecimal perTotalOutConSettleRatio;

    /**
     * 上期末累计结算比例（%）
     */
    @ApiModelProperty("上期末累计结算比例（%）")
    private BigDecimal perTotalSettleAmountRatio;

    /**
     * 上期末累计合同外结算金额（含税）(元)
     */
    @ApiModelProperty("上期末累计合同外结算金额（含税）(元) ")
    private BigDecimal perTotalTaxOutConSettleAmount;

    /**
     * 上期末累计付款义务（含税）
     */
    @ApiModelProperty("上期末累计付款义务（含税）")
    private BigDecimal perTotalTaxPaymentObligation;

    /**
     * 上期末累计结算金额（含税）（元）
     */
    @ApiModelProperty("上期末累计结算金额（含税）（元）")
    private BigDecimal perTotalTaxSettleAmount;

    /**
     * 审批完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("审批完成时间")
    private Date procCompleteTime;

    /**
     * 流程状态 0- 自由态； 1- 审批中； 2- 被驳回； 3- 已撤销； 4- 已完成
     */
    @ApiModelProperty("流程状态 0- 自由态； 1- 审批中； 2- 被驳回； 3- 已撤销； 4- 已完成")
    private String procStatus;

    /**
     * 归属最小核算项目简称
     */
    @ApiModelProperty("归属最小核算项目简称")
    private String projectAsName;

    /**
     * 归属最小核算项目项目编码
     */
    @ApiModelProperty("归属最小核算项目项目编码")
    private String projectCode;

    /**
     * 最小核算项目id
     */
    @ApiModelProperty("最小核算项目id")
    private String projectId;

    /**
     * 归属最小核算项目全称
     */
    @ApiModelProperty("归属最小核算项目全称")
    private String projectName;

    /**
     * 结算日期（年月日）：默认制单日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("结算日期（年月日）：默认制单日期")
    private Date settleDate;

    /**
     * 结算结束日期（年月日）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("结算结束日期（年月日）")
    private Date settleEndDate;

    /**
     * 结算期数（流水号）
     */
    @ApiModelProperty("结算期数（流水号）")
    private String settlePeriod;

    /**
     * 结算开始日期（年月日）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("结算开始日期（年月日）")
    private Date settleStartDate;

    /**
     * 结算类型(1-过程结算 2-完工结算)
     */
    @ApiModelProperty("结算类型(1-过程结算 2-完工结算)")
    private String settleType;

    /**
     * 合同金额（含税）=合同变更后金额
     */
    @ApiModelProperty("合同金额（含税）=合同变更后金额")
    private BigDecimal taxChangedAmount;

    /**
     * 按付款义务欠付金额(含税)（本期末累计付款义务(含税)-累计已付金额）
     */
    @ApiModelProperty("按付款义务欠付金额(含税)（本期末累计付款义务(含税)-累计已付金额）")
    private BigDecimal taxObligationOwedAmount;

    /**
     * 按结算金额欠付金额(含税)(元)（本期末累计结算金额(含税)-累计已付金额）
     */
    @ApiModelProperty("按结算金额欠付金额(含税)(元)（本期末累计结算金额(含税)-累计已付金额）")
    private BigDecimal taxSettleOwedAmount;

    /**
     * 累计已付金额(累计实付付款金额)
     */
    @ApiModelProperty("累计已付金额(累计实付付款金额)")
    private BigDecimal totalActualPaymentAmount;

    /**
     * 累计应扣回的预付款金额（本期应扣回预付款:+累计已扣回的预付款实际金额）
     */
    @ApiModelProperty("累计应扣回的预付款金额（本期应扣回预付款:+累计已扣回的预付款实际金额）")
    private BigDecimal totalImprestDeductAmount;

    /**
     * 累计已扣回的预付款实际金额
     */
    @ApiModelProperty("累计已扣回的预付款实际金额")
    private BigDecimal totalImprestDeductedAmount;

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
                .append("attachBusinessId", getAttachBusinessId())
                .append("billCode", getBillCode())
                .append("conCode", getConCode())
                .append("conId", getConId())
                .append("conName", getConName())
                .append("conType", getConType())
                .append("conUniqueId", getConUniqueId())
                .append("currentImprestDeductAmount", getCurrentImprestDeductAmount())
                .append("currentNtaxOutConSettleAmount", getCurrentNtaxOutConSettleAmount())
                .append("currentNtaxPaymentObligation", getCurrentNtaxPaymentObligation())
                .append("currentNtaxSettleAmount", getCurrentNtaxSettleAmount())
                .append("currentOutConSettleRatio", getCurrentOutConSettleRatio())
                .append("currentPaymentRatio", getCurrentPaymentRatio())
                .append("currentSettleAmountRatio", getCurrentSettleAmountRatio())
                .append("currentTaxOutConSettleAmount", getCurrentTaxOutConSettleAmount())
                .append("currentTaxPaymentObligation", getCurrentTaxPaymentObligation())
                .append("currentTaxSettleAmount", getCurrentTaxSettleAmount())
                .append("currentTotalNtaxOutConSettleAmount", getCurrentTotalNtaxOutConSettleAmount())
                .append("currentTotalNtaxPaymentObligation", getCurrentTotalNtaxPaymentObligation())
                .append("currentTotalNtaxSettleAmount", getCurrentTotalNtaxSettleAmount())
                .append("currentTotalOutConSettleRatio", getCurrentTotalOutConSettleRatio())
                .append("currentTotalSettleAmountRatio", getCurrentTotalSettleAmountRatio())
                .append("currentTotalTaxOutConSettleAmount", getCurrentTotalTaxOutConSettleAmount())
                .append("currentTotalTaxPaymentObligation", getCurrentTotalTaxPaymentObligation())
                .append("currentTotalTaxSettleAmount", getCurrentTotalTaxSettleAmount())
                .append("invoiceType", getInvoiceType())
                .append("lastProcApprover", getLastProcApprover())
                .append("nextProcApprover", getNextProcApprover())
                .append("orgId", getOrgId())
                .append("orgName", getOrgName())
                .append("partaName", getPartaName())
                .append("partbName", getPartbName())
                .append("perTotalNtaxOutConSettleAmount", getPerTotalNtaxOutConSettleAmount())
                .append("perTotalNtaxPaymentObligation", getPerTotalNtaxPaymentObligation())
                .append("perTotalNtaxSettleAmount", getPerTotalNtaxSettleAmount())
                .append("perTotalOutConSettleRatio", getPerTotalOutConSettleRatio())
                .append("perTotalSettleAmountRatio", getPerTotalSettleAmountRatio())
                .append("perTotalTaxOutConSettleAmount", getPerTotalTaxOutConSettleAmount())
                .append("perTotalTaxPaymentObligation", getPerTotalTaxPaymentObligation())
                .append("perTotalTaxSettleAmount", getPerTotalTaxSettleAmount())
                .append("procCompleteTime", getProcCompleteTime())
                .append("procStatus", getProcStatus())
                .append("projectAsName", getProjectAsName())
                .append("projectCode", getProjectCode())
                .append("projectId", getProjectId())
                .append("projectName", getProjectName())
                .append("settleDate", getSettleDate())
                .append("settleEndDate", getSettleEndDate())
                .append("settlePeriod", getSettlePeriod())
                .append("settleStartDate", getSettleStartDate())
                .append("settleType", getSettleType())
                .append("taxChangedAmount", getTaxChangedAmount())
                .append("taxObligationOwedAmount", getTaxObligationOwedAmount())
                .append("taxSettleOwedAmount", getTaxSettleOwedAmount())
                .append("totalActualPaymentAmount", getTotalActualPaymentAmount())
                .append("totalImprestDeductAmount", getTotalImprestDeductAmount())
                .append("totalImprestDeductedAmount", getTotalImprestDeductedAmount())
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
