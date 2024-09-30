package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 采购方案查询 vo
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
@ApiModel(value = "列表查询参数")
public class ProcurementSchemeListQueryVO extends PageRecive {
    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购分类")
    private Integer procurementPlanType;

    @ApiModelProperty(value =  "采购经办人")
    private Long procurementOfficer;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @NotBlank(message = "项目编号不能为空，请先选择项目")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 是否为领导，领导可看到所有采购方案
     */
    @ApiModelProperty(hidden = true)
    private Integer isLeader;

    @ApiModelProperty(value = "状态")
    private Integer state;
}
