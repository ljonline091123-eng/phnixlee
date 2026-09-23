package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

@Data
public class PostList {


    private boolean corpOrg;

    /**
     * 机构id
     */
    private String orgId;

    /**
     * 机构层级
     */
    private String orgLevel;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 机构类型
     */
    private String orgType;

    /**
     * 岗位id
     */
    private String postId;

    /**
     * 岗位名称
     */
    private String postName;

    /**
     * 上级岗位标识
     */
    private boolean superPost;
}
