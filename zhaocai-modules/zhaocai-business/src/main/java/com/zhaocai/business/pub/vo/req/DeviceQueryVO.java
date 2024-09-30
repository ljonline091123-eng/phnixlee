package com.zhaocai.business.pub.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 设备分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class DeviceQueryVO {

    @ApiModelProperty(value = "设备分类id")
    private String queryId;
}
