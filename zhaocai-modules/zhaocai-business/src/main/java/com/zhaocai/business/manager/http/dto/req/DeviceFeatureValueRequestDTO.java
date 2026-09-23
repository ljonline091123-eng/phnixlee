package com.zhaocai.business.manager.http.dto.req;

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
@AllArgsConstructor
@NoArgsConstructor
public class DeviceFeatureValueRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 设备特征项id
     */
    private String deviceFeatureId;
}
