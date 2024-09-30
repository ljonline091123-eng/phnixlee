package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AreaDivisionTreeVO {
    @ApiModelProperty(name = "编码")
    private String divisionCode;

    @ApiModelProperty(name = "名称")
    private String divisionName;

    @ApiModelProperty(value = "子类")
    private List<AreaDivisionTreeVO> children;
}
