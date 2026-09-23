package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


/**
 * 项目管理-资质分类
 * @author xwj
 */
@Getter
@Setter
@TableName(value = "ck_dict_zz")
public class ProjectCertificationType extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "父编码")
    private String parentCode;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;

    //三层： 0是菜单根节点，1是菜单子节点，2是菜单叶子节点
    @ApiModelProperty(value = "层级")
    private String type;


}
