package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 设备分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class DeviceClassVO {

    @ApiModelProperty(value = "设备分类id")
    private String id;

    @ApiModelProperty(value = "分类编码")
    private String code;

    @ApiModelProperty(value = "设备分类名称")
    private String name;

    @ApiModelProperty(value = "集合")
    private List<DeviceClassVO> children;

    public DeviceClassVO(String id,String deviceClassCode,String deviceClassName) {
        this.id = id;
        this.code = deviceClassCode;
        this.name = deviceClassName;
    }
}
