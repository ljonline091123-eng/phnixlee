package com.zhaocai.business.manager.http.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/8 15:11
 */
@Data
public class PlatDept implements Serializable {
    private static final long serialVersionUID = 3342095024366483895L;

//    public PlatDept(String deptId, String departName) {
//        this.deptId = deptId;
//        this.departName = departName;
//    }

    @ApiModelProperty(value = "部门名称")
    private String deptName;

    @ApiModelProperty(value = "负责人")
    private String leader;

    @ApiModelProperty(value = "id")
    private String deptId;

    @ApiModelProperty(value = "显示顺序")
    private String orderNum;

    @ApiModelProperty(value = "纳税识别号")
    private String unifiedSociCrdtCd;

    @ApiModelProperty(value = "所属上级组织机构")
    private String superMgmtHirchyLpCorpOrgName;

    @ApiModelProperty(value = "父部门id")
    private String parentId;

    @ApiModelProperty(value = "所属上级组织机构编码")
    private String superMgmtHirchyLpCorpOrgCode;

    @ApiModelProperty(value = "")
    private String interialId;

    @ApiModelProperty(value = "是否纳税主体")
    private String payTaxMainIndCd;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value = "")
    private String simpleName;

    @ApiModelProperty(value = "组织编号")
    private String orgCode;

    @ApiModelProperty(value = "项目类型")
    private String orgTypeCd;

    @ApiModelProperty(value = "所属二级组织机构")
    private String belgMgmtScdLvlLpCorpOrgName;

    @ApiModelProperty(value = "")
    private String addr;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "所属二级组织机构编码")
    private String belgMgmtScdLvlLpCorpOrgCode;

    @ApiModelProperty(value = "子节点")
    private List<PlatDept> children;

}
