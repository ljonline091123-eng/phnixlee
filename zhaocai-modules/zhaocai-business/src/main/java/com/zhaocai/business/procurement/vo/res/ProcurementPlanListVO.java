package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 采购计划
 *
 * @author chenming
 * @date 2024/05/24
 */
@Data
@ApiModel(value = "采购计划列表")
public class ProcurementPlanListVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "计划编号")
    private String procurementPlanCode;

    @ApiModelProperty(value = "计划名称")
    private String procurementPlanName;

    @ApiModelProperty(hidden = true)
    private Integer procurementPlanType;

    @ApiModelProperty(value = "采购层级")
    private String projectHierarchy;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "计划开始时间")
    private Date beginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "计划完成时间")
    private Date endDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "计划进场时间")
    private Date arrivalDate;

    @ApiModelProperty(value = "采购填报人")
    private String procurementReporterName;

    @ApiModelProperty(value = "采购经办人")
    private String procurementOfficerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "状态")
    private Integer state;

//    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_STATE,filedName = "state")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_SCHEME_STATE,filedName = "state")
    @ApiModelProperty(value = "状态-描述")
    private String stateText;

    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "招标进展")
    private String noticeStatus;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value =  "采购计划类别")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    private String procurementPlanTypeText;
}
