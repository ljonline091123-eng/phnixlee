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
 * 结算单对象 tb_receipt_contract_settle_list
 *
 * @author cff
 * @date 2024-09-07
 */
@Data
@TableName(value = "tb_receipt_contract_settle_list")
@ApiModel(value = "ReceiptContractSettleList对象", description = "结算单对象")
public class ReceiptContractSettleList extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 工作内容
     */
    @ApiModelProperty("工作内容")
    private String basicJob;

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
    private BigDecimal currentSettleQuantity;

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
     * 结算明细表关联id
     */
    @ApiModelProperty("结算明细表关联id")
    private String itemId;

    /**
     * 明细表唯一id
     */
    @ApiModelProperty("明细表唯一id")
    private String itemUniqueId;

    /**
     * 台账主表id
     */
    @ApiModelProperty("台账主表id")
    private String ledgerId;

    /**
     * 计量单位，如m³
     */
    @ApiModelProperty("计量单位，如m³")
    private String measureUnit;

    /**
     * 计量规则，如按体积计算
     */
    @ApiModelProperty("计量规则，如按体积计算")
    private String metrologicalRules;

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
     * 合同工程量
     */
    @ApiModelProperty("合同工程量")
    private Long quantity;

    /**
     * 租赁方式(1-日 2-月租 3-工作量)
     */
    @ApiModelProperty("租赁方式(1-日 2-月租 3-工作量)")
    private String rentMode;

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
     * 成本子目编码（物资编码）
     */
    @ApiModelProperty("成本子目编码（物资编码）")
    private String subjectDtlCode;

    /**
     * 成本子目名称（物资名称）
     */
    @ApiModelProperty("成本子目名称（物资名称）")
    private String subjectDtlName;

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
     * 单价（含税）,=单价（不含税）*（1+增值税率）=单价（含税）
     */
    @ApiModelProperty("单价（含税）,=单价（不含税）*（1+增值税率）=单价（含税）")
    private BigDecimal taxPrice;

    /**
     * 税率
     */
    @ApiModelProperty("税率")
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
                .append("basicJob", getBasicJob())
                .append("costCollectionMode", getCostCollectionMode())
                .append("currentNtaxSettleAmount", getCurrentNtaxSettleAmount())
                .append("currentSettleQuantity", getCurrentSettleQuantity())
                .append("currentTax", getCurrentTax())
                .append("currentTaxSettleAmount", getCurrentTaxSettleAmount())
                .append("currentTotalNtaxSettleAmount", getCurrentTotalNtaxSettleAmount())
                .append("currentTotalSettleQuantity", getCurrentTotalSettleQuantity())
                .append("currentTotalTax", getCurrentTotalTax())
                .append("currentTotalTaxSettleAmount", getCurrentTotalTaxSettleAmount())
                .append("itemId", getItemId())
                .append("itemUniqueId", getItemUniqueId())
                .append("ledgerId", getLedgerId())
                .append("measureUnit", getMeasureUnit())
                .append("metrologicalRules", getMetrologicalRules())
                .append("ntaxPrice", getNtaxPrice())
                .append("preTotalNtaxSettleAmount", getPreTotalNtaxSettleAmount())
                .append("preTotalSettleQuantity", getPreTotalSettleQuantity())
                .append("preTotalTax", getPreTotalTax())
                .append("preTotalTaxSettleAmount", getPreTotalTaxSettleAmount())
                .append("quantity", getQuantity())
                .append("remark", getRemark())
                .append("rentMode", getRentMode())
                .append("researchProject", getResearchProject())
                .append("settleId", getSettleId())
                .append("specs", getSpecs())
                .append("subjectCode", getSubjectCode())
                .append("subjectDtlCode", getSubjectDtlCode())
                .append("subjectDtlName", getSubjectDtlName())
                .append("subjectId", getSubjectId())
                .append("subjectName", getSubjectName())
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
