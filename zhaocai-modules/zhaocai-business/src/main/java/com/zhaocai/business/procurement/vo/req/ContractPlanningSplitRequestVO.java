package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.business.procurement.domain.MaterialsList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 合约规划拆分
 *
 * @author chenming
 * @date 2024/05/27
 */
@Data
@ApiModel(value = "合约规划拆分")
public class ContractPlanningSplitRequestVO {

    //@NotBlank(message = "拆分合约规划名称不能为空")
    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    //@NotBlank(message = "拟签约合同承包范围不能为空")
    @ApiModelProperty(value = "拟签约合同承包范围")
    private String contractScope;

    @Valid
    @NotNull(message = "物料清单不能为空")
    @ApiModelProperty(value = "物料清单")
    private List<MaterialsList> materialsLists;
}
