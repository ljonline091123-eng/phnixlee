package com.zhaocai.system.api.domain;

/**
 * @author ssy
 * @date 2024/8/8 20:27
 */
public class OrgInfoQueryDTO {

    /** 第三方部门id */
    private String thridOrgId;

    /** 是否查询组织（1是|其它否） */
    private String queryOrg;

    public String getThridOrgId() {
        return thridOrgId;
    }

    public void setThridOrgId(String thridOrgId) {
        this.thridOrgId = thridOrgId;
    }

    public String getQueryOrg() {
        return queryOrg;
    }

    public void setQueryOrg(String queryOrg) {
        this.queryOrg = queryOrg;
    }
}
