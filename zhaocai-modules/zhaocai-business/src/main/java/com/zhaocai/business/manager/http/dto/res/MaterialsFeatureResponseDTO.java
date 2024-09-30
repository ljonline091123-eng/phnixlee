package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;


/**
 * 材料分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class MaterialsFeatureResponseDTO {

    /**
     * 材料特征项编号
     */
    private String featureCode;

    /**
     * 材料特征项名称
     */
    private String featureName;

    /**
     * 材料特征项id
     */
    private String id;

    /**
     * 材料分类id
     */
    private String mtrClassId;

}
