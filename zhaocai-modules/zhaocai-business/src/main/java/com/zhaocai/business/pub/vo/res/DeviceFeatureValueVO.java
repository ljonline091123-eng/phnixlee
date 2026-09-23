package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 设备特征值
 *
 * @author chenming
 * @date 2024-08-26
 */
@Data
public class DeviceFeatureValueVO {

    @ApiModelProperty(value = "设备特征项id")
    private String deviceFeatureId;

    @ApiModelProperty(value = "特征值编号")
    private String featureValueCode;

    @ApiModelProperty(value = "特征值名称")
    private String featureValueName;

    @ApiModelProperty(value = "设备特征值id")
    private String id;
}
