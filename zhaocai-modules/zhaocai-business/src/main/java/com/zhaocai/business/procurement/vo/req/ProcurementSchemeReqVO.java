package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案请求
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class ProcurementSchemeReqVO {

    /**
     * 采购方式
     */
    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

}
