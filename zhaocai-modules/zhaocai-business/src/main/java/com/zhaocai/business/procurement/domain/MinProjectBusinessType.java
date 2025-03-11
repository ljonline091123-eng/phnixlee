package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import com.zhaocai.common.core.web.domain.BaseEntityString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


/**
 * 项目管理-业务类型
 * @author xwj
 */
@Getter
@Setter
@TableName(value = "ck_dic_business_type")
public class MinProjectBusinessType extends BaseEntityString {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id",type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
    private String id;

    @ApiModelProperty(value = "父编码")
    private String parentId;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类编码")
    private String code;


}
