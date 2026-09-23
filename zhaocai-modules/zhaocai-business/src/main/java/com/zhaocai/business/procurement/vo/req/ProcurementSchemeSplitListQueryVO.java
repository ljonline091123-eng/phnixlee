package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案拆分清单
 *
 * @author chenming
 * @date 2024/06/04
 */
@Data
public class ProcurementSchemeSplitListQueryVO extends PageRecive {

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;
}
