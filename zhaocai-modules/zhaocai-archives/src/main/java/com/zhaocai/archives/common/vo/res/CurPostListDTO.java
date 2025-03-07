package com.zhaocai.archives.common.vo.res;

import lombok.Data;

@Data
public class CurPostListDTO {

    /**
     * 当前节点组织id
     */
    private String orgId;

    /**
     * 当前节点岗位id
     */
    private String postId;

    /**
     * 当前节点岗位名称
     */
    private String postName;
}
