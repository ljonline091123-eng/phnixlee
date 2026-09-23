package com.zhaocai.business.expert.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/6/5 9:24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TPIExpertInfoVO", description = "第三方专家信息VO")
public class TPIExpertInfoVO {

    @ApiModelProperty(value =  "专家id")
    private Long expertId;

    @ApiModelProperty(value =  "专家姓名")
    private String expertName;

    @ApiModelProperty(value =  "专家手机号码")
    private String expertPhone;

    @ApiModelProperty(value =  "组织机构id")
    private Long organizationId;

    @ApiModelProperty(value =  "所属组织机构")
    private String belongOrganization;

    @ApiModelProperty(value =  "工作部门id")
    private Long departmentId;

    @ApiModelProperty(value =  "工作部门")
    private String department;

}
