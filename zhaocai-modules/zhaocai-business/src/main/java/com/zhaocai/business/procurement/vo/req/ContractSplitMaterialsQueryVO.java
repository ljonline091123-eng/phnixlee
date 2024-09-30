package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 合约拆分物料请求 VO
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
public class ContractSplitMaterialsQueryVO {

    @ApiModelProperty(value = "采购计划 id")
    private Long planId;

    @ApiModelProperty(value = "合约拆分记录id")
    private List<Long> contractSpiltIdList;
}
