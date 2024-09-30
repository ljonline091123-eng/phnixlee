package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/20 11:03
 */
@Data
public class MarketQuotePriceRequestDTO extends UnderlyingPlatformBaseDTO {

    @ApiModelProperty(value = "询价类型")
    private Integer quoteType;

    @ApiModelProperty(value = "唯一标识")
    private String quoteId;

    @ApiModelProperty(value = "项目id")
    private String projectId;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "项目联系人")
    private String contractName;

    @ApiModelProperty(value = "项目联系人电话")
    private String contractPhone;

    @ApiModelProperty(value = "清单列表")
    private List<MarketProductListRequestDTO> productListRequestDTOList;
}
