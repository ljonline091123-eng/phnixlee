package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


/**
 * 项目管理-工程类型
 * @author xwj
 */
@Getter
@Setter
@TableName(value = "ck_dic_project_type")
public class MinProjectDictProjectType extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "父编码")
    private Long parentId;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;


}
