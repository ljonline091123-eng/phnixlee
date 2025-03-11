package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 *项目-业务类型
 */
@Data
public class MinProjectBusinessTypeVO {

    public MinProjectBusinessTypeVO(String id, String parentId, String name, String code) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.code = code;
    }

    @ApiModelProperty(value = "id")
    private String id;

    @ApiModelProperty(value = "父编码")
    private String parentId;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;

    @ApiModelProperty(value = "子节点")
    List<MinProjectBusinessTypeVO> children;

}
