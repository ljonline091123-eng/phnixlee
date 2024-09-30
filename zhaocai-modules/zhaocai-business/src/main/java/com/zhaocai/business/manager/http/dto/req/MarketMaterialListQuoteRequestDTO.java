package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author ssy
 * @date 2024/9/20 17:20
 */
@Data
public class MarketMaterialListQuoteRequestDTO {

    @ApiModelProperty(value = "清单id")
    private String requireId;

}
