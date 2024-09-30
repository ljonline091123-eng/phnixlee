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
 * 周材租赁台账单据对象 tb_receipt_materials_turn_ledger
 *
 * @author cff
 * @date 2024-09-10
 */
@Data
@TableName(value = "tb_receipt_materials_turn_ledger")
@ApiModel(value = "ReceiptMaterialsTurnLedger对象", description = "周材租赁台账单据对象")
public class ReceiptMaterialsTurnLedger extends BaseEntity {

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
     * 合同编码
     */
    @ApiModelProperty("合同编码")
    private String conCode;

    /**
     * 合同名称
     */
    @ApiModelProperty("合同名称")
    private String conName;

    /**
     * 合同uninque
     */
    @ApiModelProperty("合同uninque")
    private String conUniqueId;

    /**
     * 对账结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("对账结束日期,yyyy-MM-dd HH:mm:ss")
    private Date endDate;

    /**
     * 周材台账编码
     */
    @ApiModelProperty("周材台账编码")
    private String ledgerCode;

    /**
     * 对账月
     */
    @ApiModelProperty("对账月")
    private String month;

    /**
     * 本月租金(含税)
     */
    @ApiModelProperty("本月租金(含税)")
    private BigDecimal monthRentInclude;

    /**
     * 本月租金(不含税)
     */
    @ApiModelProperty("本月租金(不含税)")
    private BigDecimal monthRentNotax;

    /**
     * 本月租金税额
     */
    @ApiModelProperty("本月租金税额")
    private BigDecimal monthRentTaxmoney;

    /**
     * 本期累计租金
     */
    @ApiModelProperty("本期累计租金")
    private BigDecimal monthRentTotal;

    /**
     * 归属本级组织
     */
    @ApiModelProperty("归属本级组织")
    private String orgName;

    /**
     * 对账周期
     */
    @ApiModelProperty("对账周期")
    private String period;

    /**
     * 归属最小核算项目编码
     */
    @ApiModelProperty("归属最小核算项目编码")
    private String projectCode;

    /**
     * 项目id
     */
    @ApiModelProperty("项目id")
    private String projectId;

    /**
     * 归属最小核算项目
     */
    @ApiModelProperty("归属最小核算项目")
    private String projectName;

    /**
     * 计租方式(数据字典：RENT_TYPE) 1算头又算尾 2算头不算尾 3算尾不算头 4头尾都不算
     */
    @ApiModelProperty("计租方式(数据字典：RENT_TYPE) 1算头又算尾 2算头不算尾 3算尾不算头 4头尾都不算")
    private String rentType;

    /**
     * 结算日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("结算日期,yyyy-MM-dd HH:mm:ss")
    private Date settleDate;

    /**
     * 结算状态,数据字典：SETTLE_STATUS
     */
    @ApiModelProperty("结算状态,数据字典：SETTLE_STATUS")
    private String settleStatus;

    /**
     * 对账开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("对账开始日期,yyyy-MM-dd HH:mm:ss")
    private Date startDate;

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
                .append("conCode", getConCode())
                .append("conName", getConName())
                .append("conUniqueId", getConUniqueId())
                .append("endDate", getEndDate())
                .append("ledgerCode", getLedgerCode())
                .append("month", getMonth())
                .append("monthRentInclude", getMonthRentInclude())
                .append("monthRentNotax", getMonthRentNotax())
                .append("monthRentTaxmoney", getMonthRentTaxmoney())
                .append("monthRentTotal", getMonthRentTotal())
                .append("orgName", getOrgName())
                .append("period", getPeriod())
                .append("projectCode", getProjectCode())
                .append("projectId", getProjectId())
                .append("projectName", getProjectName())
                .append("rentType", getRentType())
                .append("settleDate", getSettleDate())
                .append("settleStatus", getSettleStatus())
                .append("startDate", getStartDate())
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
