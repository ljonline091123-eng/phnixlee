package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


/**
 * 材料分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class MaterialsClassVO {

    @ApiModelProperty(value = "材料分类 id")
    private String id;

    @ApiModelProperty(value = "材料分类编码")
    private String code;

    @ApiModelProperty(value = "材料分类名称")
    private String name;

    @ApiModelProperty(value = "计量单位")
    private String measureUnit;

    @ApiModelProperty(value = "子集")
    private List<MaterialsClassVO> children;

    public MaterialsClassVO(String id,String mtrClassCode,String mtrClassName,String measureUnit) {
        this.id = id;
        this.code = mtrClassCode;
        this.name = mtrClassName;
        this.measureUnit = measureUnit;
    }
}
