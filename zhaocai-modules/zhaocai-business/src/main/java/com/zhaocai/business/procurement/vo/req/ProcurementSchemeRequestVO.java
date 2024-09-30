package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.domain.ProcurementSchemeBidding;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 采购方案请求
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class ProcurementSchemeRequestVO {

    @ApiModelProperty(value = "采购方案基本信息")
    private ProcurementScheme procurementScheme;

    @ApiModelProperty(value = "采购方案招标信息")
    private ProcurementSchemeBidding procurementSchemeBidding;

    @ApiModelProperty(value = "选择的合约拆分id")
    private List<Long> contractSplitIds;

}
