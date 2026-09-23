package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/9/20 11:13
 */
@Data
public class MarketProductListRequestDTO {

    @ApiModelProperty(value = "清单id")
    private String requireId;

    @ApiModelProperty(value = "商品编码")
    private String code;

    @ApiModelProperty(value = "商品名称")
    private String name;

    @ApiModelProperty(value = "商品规格")
    private String category;

    @ApiModelProperty(value = "商品单位")
    private String unitName;

    @ApiModelProperty(value = "商品数量")
    private BigDecimal quantity;

}
