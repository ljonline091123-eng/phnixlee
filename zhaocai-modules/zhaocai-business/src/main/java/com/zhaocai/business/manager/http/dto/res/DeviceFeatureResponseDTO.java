package com.zhaocai.business.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 设备特征项
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class DeviceFeatureResponseDTO {

    @ApiModelProperty(value = "设备分类id")
    private String deviceClassId;

    @ApiModelProperty(value = "设备特征项编号")
    private String featureCode;

    @ApiModelProperty(value = "设备特征项名称")
    private String featureName;

    @ApiModelProperty(value = "设备特征项id")
    private String id;
}
