package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author ssy
 * @date 2024/7/18 13:58
 */
@Data
public class MinProjectDataVO {

    @ApiModelProperty(value = "采购方案 id")
    private Long schemeId;

    @ApiModelProperty(value = "项目简称（最小核算项目名称）")
    private String minAccountFullName;

    @ApiModelProperty(value = "项目编号")
    private String projectCode;

}
