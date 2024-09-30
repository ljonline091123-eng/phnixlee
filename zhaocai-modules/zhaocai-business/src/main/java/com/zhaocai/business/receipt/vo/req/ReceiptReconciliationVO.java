package com.zhaocai.business.receipt.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author zhangxu
 * @date 2024-09-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReceiptReconciliationVO", description = "材料对账单VO")
public class ReceiptReconciliationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 材料对账单第三方id */
    @ApiModelProperty(value = "材料对账单第三方id")
    private String Id;

    /** 单据状态 */
    @ApiModelProperty(value = "单据状态")
    private String orderStatus;

    /** 归属本级组织名称 */
    @ApiModelProperty(value = "归属本级组织名称")
    private String orgName;

    /** 乙方对账人 */
    @ApiModelProperty(value = "乙方对账人")
    private String planBReconcilerName;

    /** 单据状态 */
    @ApiModelProperty(value = "单据状态")
    private String procStatus;

    /** 最小核算项目编码 */
    @ApiModelProperty(value = "最小核算项目编码")
    private String projectCode;

    /** 最小核算项目id */
    @ApiModelProperty(value = "最小核算项目id")
    private String projectId;

    /** 最小核算项目名称 */
    @ApiModelProperty(value = "最小核算项目名称")
    private String projectName;

    /** 对账单编码 */
    @ApiModelProperty(value = "对账单编码")
    private String reconciliationCode;

    /** 对账日期 */
    @ApiModelProperty(value = "对账日期")
    private String reconciliationDate;

    /** 对账周期-结束时间 */
    @ApiModelProperty(value = "对账周期-结束时间")
    private String reconciliationEndDate;

    /** 对账周期-开始时间 */
    @ApiModelProperty(value = "对账周期-开始时间")
    private String reconciliationStartDate;

    /** 供应商id */
    @ApiModelProperty(value = "供应商id")
    private String supplierId;

    /** 供应商名称 */
    @ApiModelProperty(value = "供应商名称")
    private String supplierName;

    /** 附件业务id */
    @ApiModelProperty(value = "附件业务id")
    private String attachBusinessId;

    /** 支出合同编码 */
    @ApiModelProperty(value = "支出合同编码")
    private String conCode;

    /** 支出合同id */
    @ApiModelProperty(value = "支出合同id")
    private String conId;

    /** 支出合同名称 */
    @ApiModelProperty(value = "支出合同名称")
    private String conName;

    /** 创建人 */
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private String createTime;

    /** 材料对账单详情VO列表 */
    @ApiModelProperty(value = "材料对账单详情VO列表")
    private List<ReceiptReconciliationDetailVO> dataRespList;
}
