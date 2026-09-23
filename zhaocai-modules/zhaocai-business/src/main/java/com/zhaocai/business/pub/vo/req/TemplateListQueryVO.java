package com.zhaocai.business.pub.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模板列表查询
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class TemplateListQueryVO extends PageRecive {

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "模板分类")
    private Integer templateType;

    @ApiModelProperty(value = "单位id")
    private String[] usingUnitNo;

    @ApiModelProperty(value = "合同类型")
    private String contractType;

    /**
     * 模板类型切换
     */
    @ApiModelProperty(value = "模板类型切换")
    private String switchTemplateType;
}
