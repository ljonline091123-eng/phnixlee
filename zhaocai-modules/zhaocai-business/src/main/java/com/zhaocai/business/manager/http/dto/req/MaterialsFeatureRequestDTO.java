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
public class MaterialsFeatureRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 材料分类id
     */
    private String mtrClassId;
}
