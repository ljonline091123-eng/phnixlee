package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 采购计划查询 VO
 *
 * @author chenming
 * @date 2024/05/27
 */
@Data
@ApiModel(value = "采购计划查询参数")
public class ProcurementPlanListQueryVO extends PageRecive {

    @ApiModelProperty(value = "计划编号")
    private String procurementPlanCode;

    @ApiModelProperty(value = "计划名称")
    private String procurementPlanName;

    @ApiModelProperty(value = "采购计划分类")
    private Integer procurementPlanType;

    @ApiModelProperty(value =  "项目名称")
    private String projectName;

    @ApiModelProperty(value =  "采购经办人")
    private String procurementOfficerName;

    @ApiModelProperty(hidden = true)
    private Integer state;

    @NotBlank(message = "项目编号不能为空，请先选择项目")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "招标责任单位")
    private String bidResponsibleOrg;
}
