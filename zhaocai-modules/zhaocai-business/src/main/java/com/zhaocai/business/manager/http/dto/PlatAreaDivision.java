package com.zhaocai.business.manager.http.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台区域划分
 * @author ssy
 * @date 2024/7/11 16:08
 */
@Data
public class PlatAreaDivision implements Serializable {
    private static final long serialVersionUID = 1649507187416039797L;

    @ApiModelProperty(value = "行政区域档案id")
    private String id;

    @ApiModelProperty(value = "值编码")
    private String code;

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "父级编码")
    private String parentCode;

    @ApiModelProperty(value = "父级名称")
    private String parentName;

    @ApiModelProperty(value = "显示排序")
    private String orderNum;

    @ApiModelProperty(value = "备注")
    private String remark;

}
