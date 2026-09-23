package com.zhaocai.business.receipt.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhangxu
 * @date 2024-09-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReceiptReconciliationListVO", description = "材料对账单列表查询VO")
public class ReceiptReconciliationListVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 材料对账单id
     */
    @ApiModelProperty(value = "材料对账单id")
    private Long id;

    /**
     * 材料对账单第三方id
     */
    @ApiModelProperty(value = "材料对账单第三方id")
    private String thirdId;

    /**
     * 单据状态
     */
    @ApiModelProperty(value = "单据状态")
    private String orderStatus;

    /**
     * 归属本级组织id
     */
    @ApiModelProperty(value = "归属本级组织id")
    private Long orgId;

    /**
     * 对账单位
     */
    @ApiModelProperty(value = "对账单位")
    private String orgName;

    /**
     * 乙方对账人
     */
    @ApiModelProperty(value = "乙方对账人")
    private String planBReconcilerName;

    /**
     * 单据状态
     */
    @ApiModelProperty(value = "单据状态")
    private String procStatus;

    /**
     * 最小核算项目编码
     */
    @ApiModelProperty(value = "最小核算项目编码")
    private String projectCode;

    /**
     * 最小核算项目id
     */
    @ApiModelProperty(value = "最小核算项目id")
    private String projectId;

    /**
     * 最小核算项目名称
     */
    @ApiModelProperty(value = "最小核算项目名称")
    private String projectName;

    /**
     * 对账单编码
     */
    @ApiModelProperty(value = "对账单编码")
    private String reconciliationCode;

    /**
     * 对账日期
     */
    @ApiModelProperty(value = "对账日期")
    private String reconciliationDate;

    /**
     * 对账周期-结束时间
     */
    @ApiModelProperty(value = "对账周期-结束时间")
    private String reconciliationEndDate;

    /**
     * 对账周期-开始时间
     */
    @ApiModelProperty(value = "对账周期-开始时间")
    private String reconciliationStartDate;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private String supplierId;

    /**
     * 供应商名称
     */
    @ApiModelProperty(value = "供应商名称")
    private String supplierName;

    /**
     * 附件业务id
     */
    @ApiModelProperty(value = "附件业务id")
    private String attachBusinessId;

    /**
     * 支出合同编码
     */
    @ApiModelProperty(value = "支出合同编码")
    private String conCode;

    /**
     * 支出合同id
     */
    @ApiModelProperty(value = "支出合同id")
    private String conId;

    /**
     * 支出合同名称
     */
    @ApiModelProperty(value = "支出合同名称")
    private String conName;

    /**
     * 第三方创建人
     */
    @ApiModelProperty(value = "第三方创建人")
    private String thirdCreateBy;

    /**
     * 第三方创建时间
     */
    @ApiModelProperty(value = "第三方创建时间")
    private String thirdCreateTime;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**
     * 创建人id
     */
    @ApiModelProperty(value = "创建人id")
    private Long createId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 修改人
     */
    @ApiModelProperty(value = "修改人")
    private String updateBy;

    /**
     * 修改人id
     */
    @ApiModelProperty(value = "修改人id")
    private Long updateId;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    /**
     * 删除标志(0代表存在 2代表删除)
     */
    @ApiModelProperty(value = "删除标志(0代表存在 2代表删除)")
    private String delFlag;

    /**
     * 供应商状态(0未发送、1已发送、2已签收、3已确认)
     */
    @ApiModelProperty(value = "供应商状态(0未发送、1已发送、2已签收、3已确认)")
    private String supplierStatus;
}
