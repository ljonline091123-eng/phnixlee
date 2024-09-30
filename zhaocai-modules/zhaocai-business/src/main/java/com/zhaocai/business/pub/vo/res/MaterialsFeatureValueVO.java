package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 材料特征值vo
 *
 * @author chenming
 * @date 2024-08-26
 */
@Data
public class MaterialsFeatureValueVO {

    @ApiModelProperty(value = "特征值编号")
    private String featureValueCode;

    @ApiModelProperty(value = "特征值名称")
    private String featureValueName;

    @ApiModelProperty(value = "材料特征值id")
    private String id;

    @ApiModelProperty(value = "材料特征项id")
    private String mtrFeatureId;
}
