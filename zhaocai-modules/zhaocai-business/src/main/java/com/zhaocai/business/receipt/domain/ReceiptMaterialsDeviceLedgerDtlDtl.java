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
 * 设备租赁台账详情的明细对象 tb_receipt_materials_device_ledger_dtl_dtl
 *
 * @author cff
 * @date 2024-09-10
 */
@Data
@TableName(value = "tb_receipt_materials_device_ledger_dtl_dtl")
@ApiModel(value = "ReceiptMaterialsDeviceLedgerDtlDtl对象", description = "设备租赁台账详情的明细对象")
public class ReceiptMaterialsDeviceLedgerDtlDtl extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 台班结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("台班结束时间,yyyy-MM-dd HH:mm:ss")
    private Date endDatetime;

    /**
     * 设备台账详情id
     */
    @ApiModelProperty("设备台账详情id")
    private String ledgerDtlId;

    /**
     * 设备台账id
     */
    @ApiModelProperty("设备台账id")
    private String ledgerId;

    /**
     * 台班开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("台班开始时间,yyyy-MM-dd HH:mm:ss")
    private Date startDatetime;

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
                .append("endDatetime", getEndDatetime())
                .append("ledgerDtlId", getLedgerDtlId())
                .append("ledgerId", getLedgerId())
                .append("startDatetime", getStartDatetime())
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
