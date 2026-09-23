package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceFeatureVO {

    @ApiModelProperty(value = "设备分类id")
    private String deviceClassId;

    @ApiModelProperty(value = "设备特征项编号")
    private String featureCode;

    @ApiModelProperty(value = "设备特征项名称")
    private String featureName;

    @ApiModelProperty(value = "设备特征项id")
    private String id;
}
