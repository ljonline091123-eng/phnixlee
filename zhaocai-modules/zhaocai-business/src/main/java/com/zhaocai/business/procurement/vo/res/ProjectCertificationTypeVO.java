package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/7/13 16:11
 */
@Data
public class ProjectCertificationTypeVO {

    public ProjectCertificationTypeVO(Long id, String parentCode, String name, String code, String type) {
        this.id = id;
        this.parentCode = parentCode;
        this.name = name;
        this.code = code;
        this.type = type;
    }

    @ApiModelProperty(value = "主键id")
    private Long id;

    @ApiModelProperty(value = "父编码")
    private String parentCode;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;

    //三层： 0是菜单根节点，1是菜单子节点，2是菜单叶子节点
    @ApiModelProperty(value = "层级")
    private String type;

    @ApiModelProperty(value = "子节点")
    List<ProjectCertificationTypeVO> children;

}
