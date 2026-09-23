package com.zhaocai.business.manager.http.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * 材料分类列表请求 dto
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
@AllArgsConstructor
public class MaterialsFeatureValueRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 材料特征项id
     */
    private String mtrFeatureId;
}
