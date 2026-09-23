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
 * 设备租赁台账详情对象 tb_receipt_materials_device_ledger_dtl
 *
 * @author cff
 * @date 2024-09-10
 */
@Data
@TableName(value = "tb_receipt_materials_device_ledger_dtl")
@ApiModel(value = "ReceiptMaterialsDeviceLedgerDtl对象", description = "设备租赁台账详情对象")
public class ReceiptMaterialsDeviceLedgerDtl extends BaseEntity {

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
     * 施工结束
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("施工结束,yyyy-MM-dd HH:mm:ss")
    private Date endDatetime;

    /**
     * 设备台账id
     */
    @ApiModelProperty("设备台账id")
    private String ledgerId;

    /**
     * 台班类型,数据字典：MTR_MACH_TYPE
     */
    @ApiModelProperty("台班类型,数据字典：MTR_MACH_TYPE")
    private String machineType;

    /**
     * 物资编码
     */
    @ApiModelProperty("物资编码")
    private String materialCode;

    /**
     * 物资名称
     */
    @ApiModelProperty("物资名称")
    private String materialName;

    /**
     * 规格型号
     */
    @ApiModelProperty("规格型号")
    private String specificationModel;

    /**
     * 停滞台班结算比例
     */
    @ApiModelProperty("停滞台班结算比例")
    private Long stagnateMachineSettleRatio;

    /**
     * 施工开始
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("施工开始,yyyy-MM-dd HH:mm:ss")
    private Date startDatetime;

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
     * 工作时长
     */
    @ApiModelProperty("工作时长")
    private BigDecimal workHours;

    /**
     * 工作量
     */
    @ApiModelProperty("工作量")
    private BigDecimal workload;

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
                .append("deviceCode", getDeviceCode())
                .append("deviceName", getDeviceName())
                .append("endDatetime", getEndDatetime())
                .append("ledgerId", getLedgerId())
                .append("machineType", getMachineType())
                .append("materialCode", getMaterialCode())
                .append("materialName", getMaterialName())
                .append("specificationModel", getSpecificationModel())
                .append("stagnateMachineSettleRatio", getStagnateMachineSettleRatio())
                .append("startDatetime", getStartDatetime())
                .append("taxRate", getTaxRate())
                .append("unitIncludeTax", getUnitIncludeTax())
                .append("unitNoTax", getUnitNoTax())
                .append("unitTaxMoney", getUnitTaxMoney())
                .append("workHours", getWorkHours())
                .append("workload", getWorkload())
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
