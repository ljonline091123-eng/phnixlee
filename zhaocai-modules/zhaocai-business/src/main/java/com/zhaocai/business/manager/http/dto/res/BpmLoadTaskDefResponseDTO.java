package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

import java.util.List;


/**
 * 加载定义接口 返回
 */
@Data
public class BpmLoadTaskDefResponseDTO {


    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 运行时用户对象
     */
    private List<UserList> userList;
    /**
     * 运行时岗位对象
     */
    private List<PostList> postList;

    private String taskKey;

    /**
     * 节点标识
     */
    private String nodeKey;

    /**
     * 节点标识
     */
    private String taskName;

    /**
     * 是否完成
     */
    private boolean completed;

    /**
     * 运行时岗位对象
     */
    private List<TaskPost> taskPost;

    /**
     * 层级
     */
    private int level;
}
