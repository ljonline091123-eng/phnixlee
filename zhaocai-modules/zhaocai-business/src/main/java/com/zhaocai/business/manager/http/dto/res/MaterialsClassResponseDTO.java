package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;


/**
 * 材料分类列表
 *
 * @author chenming
 * @date 2024-08-11
 */
@Data
public class MaterialsClassResponseDTO {

    /**
     *
     */
    private Boolean addChildFlag;

    /**
     * 材料分类id
     */
    private String id;

    /**
     * 计量单位
     */
    private String measureUnit;

    /**
     * 材料分类编码
     */
    private String mtrClassCode;

    /**
     * 材料分类名称
     */
    private String mtrClassName;

    /**
     * 父节点id
     */
    private String parentId;

}
