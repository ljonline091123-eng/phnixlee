package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

/**
 * 运行时用户对象
 */
@Data
public class CurCandidateListDTO {

    /**
     * 是否指派用户
     */
    private boolean appoint;
    /**
     * 是否完成处理
     */
    private boolean completed;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;
}
