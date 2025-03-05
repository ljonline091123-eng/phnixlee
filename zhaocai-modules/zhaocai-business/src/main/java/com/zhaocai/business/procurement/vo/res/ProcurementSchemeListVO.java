package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 采购方案列表
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class ProcurementSchemeListVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购计划类别")
    private Integer procurementPlanType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    @ApiModelProperty(value = "采购计划类别-文本 ")
    private String procurementPlanTypeText;

    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_TYPE,filedName = "procurementType")
    @ApiModelProperty(value = "采购方式-文本")
    private String procurementTypeText;

    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "状态")
    private Integer state;

    @ApiModelProperty(value = "项目编码")
    private String projectCode;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "项目简称")
    private String minAccountSimpleName;

    @ApiModelProperty(value = "状态-文本")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_SCHEME_STATE,filedName = "state")
    private String stateText;
}
