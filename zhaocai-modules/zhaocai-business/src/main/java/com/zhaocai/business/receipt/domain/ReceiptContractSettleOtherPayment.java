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
 * 其他款项结算明细对象 tb_receipt_contract_settle_other_payment
 *
 * @author cff
 * @date 2024-09-07
 */
@Data
@TableName(value = "tb_receipt_contract_settle_other_payment")
@ApiModel(value = "ReceiptContractSettleOtherPayment对象", description = "其他款项结算明细对象")
public class ReceiptContractSettleOtherPayment extends BaseEntity {

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
     * 金额（不含税）
     */
    @ApiModelProperty("金额（不含税）")
    private BigDecimal ntaxAmount;

    /**
     * 清单名称
     */
    @ApiModelProperty("清单名称")
    private String paymentName;

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
     * 成本科目编码
     */
    @ApiModelProperty("成本科目编码")
    private String subjectCode;

    /**
     * 成本科目档案ID
     */
    @ApiModelProperty("成本科目档案ID")
    private String subjectId;

    /**
     * 成本科目名称
     */
    @ApiModelProperty("成本科目名称")
    private String subjectName;

    /**
     * 税额
     */
    @ApiModelProperty("税额")
    private BigDecimal tax;

    /**
     * 金额（含税）
     */
    @ApiModelProperty("金额（含税）")
    private BigDecimal taxAmount;

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
                .append("ntaxAmount", getNtaxAmount())
                .append("paymentName", getPaymentName())
                .append("remark", getRemark())
                .append("researchProject", getResearchProject())
                .append("settleId", getSettleId())
                .append("subjectCode", getSubjectCode())
                .append("subjectId", getSubjectId())
                .append("subjectName", getSubjectName())
                .append("tax", getTax())
                .append("taxAmount", getTaxAmount())
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
