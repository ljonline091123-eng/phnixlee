package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 合约规划拆分对象 tb_contract_planning_split
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_contract_planning_split")
public class ContractPlanningSplit extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购计划id
     */
    @ApiModelProperty(value = "采购计划 id")
    private Long procurementPlanId;

    /**
     * 合约规划编码
     */
    @ApiModelProperty(value = "合约规划编码")
    private String contractPlanningCode;

    /**
     * 合约规划名称
     */
    @ApiModelProperty(value = "合约规划名称")
    private String contractPlanningName;

    /**
     * 拆分合约规划名称
     */
    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    /**
     * 拟签约合同拆包范围
     */
    @ApiModelProperty(value = "拟签约合同拆包范围")
    private String contractScope;

    /**
     * 是否用尽
     */
    @ApiModelProperty(value = "是否用尽")
    private Integer isUseUp;


    /**
     * 计划金额
     */
    @ApiModelProperty(value = "计划金额")
    private BigDecimal totalPlanAmount;

    /**
     * 使用金额
     */
    @ApiModelProperty(value = "使用金额")
    private BigDecimal totalUsedAmount;
}
