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
 * 结算机械台班明细对象 tb_receipt_contract_settle_mechanical
 *
 * @author cff
 * @date 2024-09-07
 */
@Data
@TableName(value = "tb_receipt_contract_settle_mechanical")
@ApiModel(value = "ReceiptContractSettleMechanical对象", description = "结算机械台班明细对象")
public class ReceiptContractSettleMechanical extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）
     */
    @ApiModelProperty("成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）")
    private String costCollectionMode;

    /**
     * 本期结算金额(不含税)
     */
    @ApiModelProperty("本期结算金额(不含税)")
    private BigDecimal currentNtaxSettleAmount;

    /**
     * 本期结算工程量
     */
    @ApiModelProperty("本期结算工程量")
    private Long currentSettleQuantity;

    /**
     * 本期结算税额
     */
    @ApiModelProperty("本期结算税额")
    private BigDecimal currentTax;

    /**
     * 本期结算金额(含税)
     */
    @ApiModelProperty("本期结算金额(含税)")
    private BigDecimal currentTaxSettleAmount;

    /**
     * 本期末累计结算金额(不含税)
     */
    @ApiModelProperty("本期末累计结算金额(不含税)")
    private BigDecimal currentTotalNtaxSettleAmount;

    /**
     * 本期末累计结算工程量
     */
    @ApiModelProperty("本期末累计结算工程量")
    private Long currentTotalSettleQuantity;

    /**
     * 本期末累计结算税额
     */
    @ApiModelProperty("本期末累计结算税额")
    private BigDecimal currentTotalTax;

    /**
     * 本期末累计结算金额(含税)
     */
    @ApiModelProperty("本期末累计结算金额(含税)")
    private BigDecimal currentTotalTaxSettleAmount;

    /**
     * 设备编码
     */
    @ApiModelProperty("设备编码")
    private String deviceCode;

    /**
     * 设备名称
     */
    @ApiModelProperty("设备名称")
    private String deviceName;

    /**
     * 计量单位
     */
    @ApiModelProperty("计量单位")
    private String measureUnit;

    /**
     * 单价（不含税）
     */
    @ApiModelProperty("单价（不含税）")
    private BigDecimal ntaxPrice;

    /**
     * 上期末结算金额(不含税)
     */
    @ApiModelProperty("上期末结算金额(不含税)")
    private BigDecimal preTotalNtaxSettleAmount;

    /**
     * 上期末结算工程量
     */
    @ApiModelProperty("上期末结算工程量")
    private Long preTotalSettleQuantity;

    /**
     * 上期末结算税额
     */
    @ApiModelProperty("上期末结算税额")
    private BigDecimal preTotalTax;

    /**
     * 上期末结算金额(含税)
     */
    @ApiModelProperty("上期末结算金额(含税)")
    private BigDecimal preTotalTaxSettleAmount;

    /**
     * 研发项目(成本归集方式非“工程施工合同履约成本” 选择)
     */
    @ApiModelProperty("研发项目(成本归集方式非“工程施工合同履约成本” 选择)")
    private String researchProject;

    /**
     * 结算单ID,是contract_settlement的id
     */
    @ApiModelProperty("结算单ID,是contract_settlement的id")
    private String settleId;

    /**
     * 规格型号
     */
    @ApiModelProperty("规格型号")
    private String specs;

    /**
     * 成本科目编码
     */
    @ApiModelProperty("成本科目编码")
    private String subjectCode;

    /**
     * 成本科目档案ID,关联xxx表
     */
    @ApiModelProperty("成本科目档案ID,关联xxx表")
    private String subjectId;

    /**
     * 成本科目名称
     */
    @ApiModelProperty("成本科目名称")
    private String subjectName;

    /**
     * 结算明细表关联id
     */
    @ApiModelProperty("结算明细表关联id")
    private String tableId;

    /**
     * 明细表唯一id
     */
    @ApiModelProperty("明细表唯一id")
    private String tableUniqueId;

    /**
     * 单价（含税）
     */
    @ApiModelProperty("单价（含税）")
    private BigDecimal taxPrice;

    /**
     * 税率(%)
     */
    @ApiModelProperty("税率(%)")
    private BigDecimal taxRate;

    /**
     * 父级id
     */
    @ApiModelProperty("父级id")
    private String parntId;

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
                .append("costCollectionMode", getCostCollectionMode())
                .append("currentNtaxSettleAmount", getCurrentNtaxSettleAmount())
                .append("currentSettleQuantity", getCurrentSettleQuantity())
                .append("currentTax", getCurrentTax())
                .append("currentTaxSettleAmount", getCurrentTaxSettleAmount())
                .append("currentTotalNtaxSettleAmount", getCurrentTotalNtaxSettleAmount())
                .append("currentTotalSettleQuantity", getCurrentTotalSettleQuantity())
                .append("currentTotalTax", getCurrentTotalTax())
                .append("currentTotalTaxSettleAmount", getCurrentTotalTaxSettleAmount())
                .append("deviceCode", getDeviceCode())
                .append("deviceName", getDeviceName())
                .append("measureUnit", getMeasureUnit())
                .append("ntaxPrice", getNtaxPrice())
                .append("preTotalNtaxSettleAmount", getPreTotalNtaxSettleAmount())
                .append("preTotalSettleQuantity", getPreTotalSettleQuantity())
                .append("preTotalTax", getPreTotalTax())
                .append("preTotalTaxSettleAmount", getPreTotalTaxSettleAmount())
                .append("remark", getRemark())
                .append("researchProject", getResearchProject())
                .append("settleId", getSettleId())
                .append("specs", getSpecs())
                .append("subjectCode", getSubjectCode())
                .append("subjectId", getSubjectId())
                .append("subjectName", getSubjectName())
                .append("tableId", getTableId())
                .append("tableUniqueId", getTableUniqueId())
                .append("taxPrice", getTaxPrice())
                .append("taxRate", getTaxRate())
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
