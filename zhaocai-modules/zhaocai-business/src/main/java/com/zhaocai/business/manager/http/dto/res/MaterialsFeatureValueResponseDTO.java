package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

/**
 * 材料特征值
 *
 * @author chenming
 * @date 2024-08-26
 */
@Data
public class MaterialsFeatureValueResponseDTO {
    /**
     * 特征值编号
     */
    private String featureValueCode;

    /**
     * 特征值名称
     */
    private String featureValueName;

    /**
     * 材料特征项id
     */
    private String id;

    /**
     * 材料特征项id
     */
    private String mtrFeatureId;
}
