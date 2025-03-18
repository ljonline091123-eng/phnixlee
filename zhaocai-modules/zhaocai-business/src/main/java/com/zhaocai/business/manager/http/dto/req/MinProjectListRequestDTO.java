package com.zhaocai.business.manager.http.dto.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 项目列表请求参数
 * @author ssy
 * @date 2024/9/20 15:42
 */
@Data
public class MinProjectListRequestDTO  extends PageRecive {

    @ApiModelProperty(value = "最小核算项目编码")
    private String minAccountCode;

    @ApiModelProperty(value = "项目全称")
    private String minAccountFullName;

    @ApiModelProperty(value = "归属管理组织-部门id")
    private String deptId;

    @ApiModelProperty(value = "归属管理组织-第三方部门id")
    private String managementOrgId;

}
