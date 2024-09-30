package com.zhaocai.business.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ssy
 * @date 2024/7/9 15:17
 */
@Data
@NoArgsConstructor
public class PlatDeptTreeNodeDTO {

    @ApiModelProperty(value = "节点 ID")
    private Long nodeId;

    @ApiModelProperty(value = "父节点 ID")
    private Long parentNodeId;

    @ApiModelProperty(value = "部门名称")
    private String deptName;

    @ApiModelProperty(value = "子节点")
    List<PlatDeptTreeNodeDTO> children;

}
