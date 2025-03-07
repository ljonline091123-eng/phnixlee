package com.zhaocai.archives.common.vo.res;


import lombok.Data;

import java.util.List;


/**
 * 流程服务-初始化返回
 */
@Data
public class BpmInitializeResponseDTO {

    /**
     * 下一任务id
     */
    private String nextTaskId;

    /**
     * 操作初始化返回的讨论id
     */
    private String discussId;

    /**
     * 是否可以弃审
     */
    private boolean discardable;

    /**
     * 下一任务是否设置为指派
     */
    private boolean nextAppointable;

    /**
     * 流程状态
     */
    private String processStatus;

    /**
     * 是否可以讨论
     */
    private boolean discussable;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 是否可以后加签
     */
    private boolean canPostSign;

    /**
     * 是否可以分享
     */
    private boolean sharable;

    /**
     * 上一操作用户列表
     */
    private List<PrevOperateUserListDTO> prevOperateUserList;

    /**
     * 当前运行时用户对象
     */
    private List<CurCandidateListDTO> curCandidateList;

    /**
     * 是否可以提交
     */
    private boolean submitable;

    /**
     * 当前岗位列表
     */
    private List<CurPostListDTO> curPostList;

    /**
     * 已完成任务列表
     */
    private List<CompletedTaskListDTO> completedTaskList;

    /**
     * 下一运行时用户对象
     */
    private List<NextCandidateListDTO> nextCandidateList;

    /**
     * 流程实例id
     */
    private String processId;

    /**
     * 是否可以前加签
     */
    private boolean canPreSign;

    /**
     * 最后运行时用户对象
     */
    private List<LastestCandidateListDTO> lastestCandidateList;

    /**
     * 是否可以附言
     */
    private boolean postscriptable;

    /**
     * 是否可以催办
     */
    private boolean urgable;

    /**
     * 上一操作任务id
     */
    private String prevOperateTaskId;

    /**
     * 最后操作任务id
     */
    private String lastestTaskId;

    /**
     * 当前任务id
     */
    private String curTaskId;

    /**
     * 附言id
     */
    private String postscriptId;

    /**
     * 是否可以撤销
     */
    private boolean revokable;

    /**
     * 是否可以审批
     */
    private boolean auditable;
}
