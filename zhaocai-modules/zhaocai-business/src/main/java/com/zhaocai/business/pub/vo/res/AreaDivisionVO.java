package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 区域划分
 *
 * @author chenming
 * @date 2024-07-12
 */
@Data
public class AreaDivisionVO {

    @ApiModelProperty(name = "编码")
    private String divisionCode;

    @ApiModelProperty(name = "名称")
    private String divisionName;
}
