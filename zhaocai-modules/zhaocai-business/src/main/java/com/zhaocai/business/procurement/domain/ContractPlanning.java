package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 合约规划对象 tb_contract_planning
 *
 * @author chenming
 * @date 2024-06-17
 */
@Data
@TableName(value = "tb_contract_planning")
public class ContractPlanning extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购计划 id
     */
    @ApiModelProperty(hidden = true)
    private Long planId;

    /**
     * 招标责任单位
     */
    @ApiModelProperty(value = "招标责任单位")
    private String bidResponsibleOrg;

    /**
     * 招标责任单位名称
     */
    @ApiModelProperty(value = "招标责任单位名称")
    private String bidResponsibleOrgName;

    /**
     * 合约规划编码
     */
//    @NotBlank(message = "合约规划编码不能为空")
    @ApiModelProperty(value = "合约规划编码")
    private String contractPlanningCode;

    /**
     * 合约规划id
     */
//    @NotBlank(message = "合约规划的合约规划id不能为空")
    @ApiModelProperty(value = "合约规划id")
    private String contractPlanningId;

    /**
     * 合约规划名称
     */
//    @NotBlank(message = "合约规划的合约规划名称不能为空")
    @ApiModelProperty(value = "合约规划名称")
    private String contractPlanningName;

    /**
     * 项目id
     */
//    @NotBlank(message = "合约规划的项目id不能为空")
    @ApiModelProperty(value = "项目id")
    private String projectId;

    /**
     * 项目编号
     */
//    @NotBlank(message = "合约规划的项目编号不能为空")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 项目名称
     */
//    @NotBlank(message = "合约规划的项目名称不能为空")
    @ApiModelProperty(value = "项目名称")
    private String projectName;

    /**
     * 合约规划类别
     */
//    //@NotNull(message = "合约规划的合约规划类别不能为空")
    @ApiModelProperty(value = "合约规划类别")
    private Integer contractPlanningCategory;

    /**
     * 合约规划类别名称
     */
//    @NotBlank(message = "合约规划的合约规划类别名称不能为空")
    @ApiModelProperty(value = "合约规划类别名称")
    private String contractPlanningCategoryName;

    /**
     * 规划金额（含税）
     */
    //@NotNull(message = "合约规划的规划金额（含税）不能为空")
    @ApiModelProperty(value = "规划金额（含税）")
    private BigDecimal plannedAmountInclTax;

    /**
     * 已发生规划金额
     */
    //@NotNull(message = "合约规划的已发生规划金额不能为空")
    @ApiModelProperty(value = "已发生规划金额")
    private BigDecimal incurredPlannedAmount;

    /**
     * 规划余量
     */
    //@NotNull(message = "合约规划的规划余量不能为空")
    @ApiModelProperty(value = "规划余量")
    private BigDecimal planningBalance;

    /**
     * 拟定招标方式编码
     */
//    @NotBlank(message = "合约规划的拟定招标方式编码不能为空")
    @ApiModelProperty(value = "拟定招标方式编码")
    private String biddingMethodCode;

    /**
     * 拟定招标方式名称
     */
//    @NotBlank(message = "合约规划的拟定招标方式名称不能为空")
    @ApiModelProperty(value = "拟定招标方式名称")
    private String biddingMethodName;

    /**
     * 交易标的物
     */
    @ApiModelProperty(value = "交易标的物")
    private String subjectMatter;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌")
    private String brand;
}
