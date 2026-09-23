package com.zhaocai.business.pub.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织机构
 *
 * @author chenming
 * @date 2024-06-29
 */
@Data
@NoArgsConstructor
public class OrganizationVO {

    @ApiModelProperty(value = "组织机构编码")
    private String organizationCode;

    @ApiModelProperty(value = "组织机构名称")
    private String organizationName;

    @ApiModelProperty(value = "组织机构Id")
    private Long organizationId;

    @ApiModelProperty(value = "组织返回类型")
    private String organizationType;

    @ApiModelProperty(value = "子集")
    private List<OrganizationVO> children;

    public OrganizationVO(String organizationCode,String organizationName) {
        this.organizationCode = organizationCode;
        this.organizationName = organizationName;
        this.children = new ArrayList<>();
    }
}
