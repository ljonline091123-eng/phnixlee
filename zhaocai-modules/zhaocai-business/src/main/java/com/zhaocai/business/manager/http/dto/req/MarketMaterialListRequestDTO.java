package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/20 15:42
 */
@Data
public class MarketMaterialListRequestDTO extends UnderlyingPlatformBaseDTO {

    @ApiModelProperty(value = "计划id")
    private String planId;

    @ApiModelProperty(value = "项目id")
    private String projectId;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "项目联系人")
    private String contractName;

    @ApiModelProperty(value = "项目联系人电话")
    private String contractPhone;

    @ApiModelProperty(value = "清单列表")
    private List<MarketProductListRequestDTO> list;

}
