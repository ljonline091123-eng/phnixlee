package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/8/19 16:10
 */
@Data
@ApiModel(value = "CompMaterialsVO", description = "综合物料清单信息VO")
public class CompMaterialsVO extends AdviceObject {

    @ApiModelProperty(value = "采购计划id")
    private Long planId;

    /**
     * 价格类型 {@link com.zhaocai.business.common.enums.PriceTypeEnum}
     */
    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "合约规划名称")
    private String contractPlanningName;

    @ApiModelProperty(value = "计划里的清单内容")
    private List<CompContractSplitMaterialsVO> compVOList;

}
