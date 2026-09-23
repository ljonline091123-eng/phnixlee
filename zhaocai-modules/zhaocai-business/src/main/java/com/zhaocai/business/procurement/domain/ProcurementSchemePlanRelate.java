package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案-采购计划对应关系对象 tb_procurement_scheme_plan_relate
 * 
 * @author chenming
 * @date 2024-05-29
 */
@Data
@TableName(value = "tb_procurement_scheme_plan_relate")
public class ProcurementSchemePlanRelate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 采购方案id */
    @ApiModelProperty(name = "采购方案id")
    private Long procurementSchemeId;

    /** 采购计划id */
    @ApiModelProperty(name = "采购计划id")
    private Long procurementPlanId;

    /** 拆分合约 id */
    @ApiModelProperty(name = "拆分合约 id")
    private Long contractSplitId;
}
