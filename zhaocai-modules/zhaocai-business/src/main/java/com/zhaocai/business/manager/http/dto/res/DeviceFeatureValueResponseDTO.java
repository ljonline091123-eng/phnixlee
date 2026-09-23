package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

/**
 * 设备特征值
 *
 * @author chenming
 * @date 2024-08-26
 */
@Data
public class DeviceFeatureValueResponseDTO {

    /**
     * 设备特征项id
     */
    private String deviceFeatureId;

    /**
     * 特征值编号
     */
    private String featureValueCode;

    /**
     * 特征值名称
     */
    private String featureValueName;

    /**
     * 设备特征值id
     */
    private String id;
}
