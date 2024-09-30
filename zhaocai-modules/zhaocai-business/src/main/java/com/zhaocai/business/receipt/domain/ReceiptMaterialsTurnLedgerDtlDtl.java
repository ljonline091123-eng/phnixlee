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
 * 周材租赁台账详情的明细对象 tb_receipt_materials_turn_ledger_dtl_dtl
 *
 * @author cff
 * @date 2024-09-10
 */
@Data
@TableName(value = "tb_receipt_materials_turn_ledger_dtl_dtl")
@ApiModel(value = "ReceiptMaterialsTurnLedgerDtlDtl对象", description = "周材租赁台账详情的明细对象")
public class ReceiptMaterialsTurnLedgerDtlDtl extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 金额(含税)
     */
    @ApiModelProperty("金额(含税)")
    private BigDecimal amtIncludeTax;

    /**
     * 金额(不含税)
     */
    @ApiModelProperty("金额(不含税)")
    private BigDecimal amtNoTax;

    /**
     * 金额税额
     */
    @ApiModelProperty("金额税额")
    private BigDecimal amtTaxMoney;

    /**
     * 进场日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("进场日期,yyyy-MM-dd HH:mm:ss")
    private Date inDate;

    /**
     * 周转材台账详情id
     */
    @ApiModelProperty("周转材台账详情id")
    private String ledgerDtlId;

    /**
     * 周转材台账id
     */
    @ApiModelProperty("周转材台账id")
    private String ledgerId;

    /**
     * 进场数量
     */
    @ApiModelProperty("进场数量")
    private BigDecimal inNum;

    /**
     * 物资编码
     */
    @ApiModelProperty("物资编码")
    private String materialCode;

    /**
     * 税率
     */
    @ApiModelProperty("税率")
    private BigDecimal taxRate;

    /**
     * 单价(含税)
     */
    @ApiModelProperty("单价(含税)")
    private BigDecimal unitIncludeTax;

    /**
     * 单价(不含税)
     */
    @ApiModelProperty("单价(不含税)")
    private BigDecimal unitNoTax;

    /**
     * 单价税额
     */
    @ApiModelProperty("单价税额")
    private BigDecimal unitTaxMoney;

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
                .append("amtIncludeTax", getAmtIncludeTax())
                .append("amtNoTax", getAmtNoTax())
                .append("amtTaxMoney", getAmtTaxMoney())
                .append("inDate", getInDate())
                .append("ledgerDtlId", getLedgerDtlId())
                .append("ledgerId", getLedgerId())
                .append("inNum", getInNum())
                .append("materialCode", getMaterialCode())
                .append("taxRate", getTaxRate())
                .append("unitIncludeTax", getUnitIncludeTax())
                .append("unitNoTax", getUnitNoTax())
                .append("unitTaxMoney", getUnitTaxMoney())
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
