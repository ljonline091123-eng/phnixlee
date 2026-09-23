package com.zhaocai.business.manager.http.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


/**
 * 材料分类列表请求 dto
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaterialsClassRequestDTO extends UnderlyingPlatformBaseDTO{

    private String mtrClassCode = "";
}
