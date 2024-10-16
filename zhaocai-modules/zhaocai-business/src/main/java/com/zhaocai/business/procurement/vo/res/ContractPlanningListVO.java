package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.bidding.vo.res.ContractPlanningNoticeVO;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.procurement.domain.ContractPlanningPushRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 项目合约规划
 *
 * @author chenming
 * @date 2024/05/27
 */
@Data
@ApiModel(value = "项目合约规划")
@NoArgsConstructor
@AllArgsConstructor
public class ContractPlanningListVO extends AdviceObject {

    @ApiModelProperty(value = "招标责任单位")
    private String bidResponsibleOrg;

    @ApiModelProperty(value = "招标责任单位名称")
    private String bidResponsibleOrgName;

    @ApiModelProperty(value = "项目合约id")
    private String contractPlanningId;

    @ApiModelProperty(value = "项目合约编码")
    private String contractPlanningCode;

    @ApiModelProperty(value = "项目合约名称")
    private String contractPlanningName;

    @ApiModelProperty(value = "项目合约类别")
    private Integer contractPlanningCategory;

    @ApiModelProperty(value = "规划金额")
    private BigDecimal plannedAmountInclTax;

    @ApiModelProperty(value = "已发生规划金额")
    private BigDecimal incurredPlannedAmount;

    @ApiModelProperty(value = "规划余量")
    private BigDecimal planningBalance;

    @ApiModelProperty(value = "拟定招标方式编码")
    private String biddingMethodCode;

    @ApiModelProperty(value = "拟定招标方式名称")
    private String biddingMethodName;

    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "招标时间")
    private String biddingTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "进场时间")
    private Date enterIntoTime;

    @ApiModelProperty(value = "招标状态")
    private String biddingState;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @MoneyFormat(filedName = "plannedAmountInclTax",scale = 2)
    @ApiModelProperty(value = "规划金额")
    private String plannedAmountInclTaxText;

    @MoneyFormat(filedName = "incurredPlannedAmount",scale = 2)
    @ApiModelProperty(value = "已发生规划金额")
    private String incurredPlannedAmountText;

    @MoneyFormat(filedName = "planningBalance",scale = 2)
    @ApiModelProperty(value = "规划余量")
    private String planningBalanceText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "contractPlanningCategory")
    @ApiModelProperty(value = "项目合约类别名称")
    private String contractPlanningCategoryName;

    @DictCache(dictBizEnum = DictBizEnum.CONTRACT_BIDDING_STATE,filedName = "biddingState")
    @ApiModelProperty(value = "招标状态-文本")
    private String biddingStateText;

    @ApiModelProperty("推送状态（0未推送 1已推送）")
    private Integer pushStatus;

    @ApiModelProperty("招标对象推送状态")
    private List<ContractPlanningNoticeVO> contractPlanningNoticeVOList;

}
