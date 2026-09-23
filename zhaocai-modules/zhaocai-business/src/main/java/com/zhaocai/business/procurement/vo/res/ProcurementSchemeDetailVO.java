package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 采购方案详情
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
@Builder
public class ProcurementSchemeDetailVO extends AdviceObject {

    @ApiModelProperty(value = "采购方案基本信息")
    private ProcurementSchemeVO procurementScheme;

    @ApiModelProperty(value = "采购方案招标文件")
    private ProcurementSchemeBiddingVO procurementSchemeBidding;

    @ApiModelProperty(value = "采购方案合约规划")
    private List<ProcurementContractPlanListVO> contractPlanList;

    @ApiModelProperty(value = "合约拆分id")
    private List<Long> contractSplitIdList;
}
