package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

/**
 * 设备分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class DeviceClassListResponseDTO {
    /**
     * 设备分类编码
     */
    private String deviceClassCode;

    /**
     * 设备分类名称
     */
    private String deviceClassName;

    /**
     * 设备分类id
     */
    private String id;

    /**
     * 父节点id
     */
    private String parentId;
}
