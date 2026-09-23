package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessStateEnum {

    /**
     * @description
     * 自由态:新增未提交，提交后被其他节点审批人驳回至提交人
     * 审批中:提交人提交后，其他节点审批人，审批后都是审批中，
     * 已撤销:提交人和审批人都可以撤销当前操作
     * 已完成:流程已经全部审批完(表示最后一个节点任务审批通过)
     * 作废:就是删除
     * 弃审:在当前节点，放弃本人的操作，最后一个审批人弃审单据会由审批完成变为审批中
     * */

    FREEDOM("0", "自由态", true),
    AUDITING("1", "审批中", false),
    // 被驳回 表示某个节点任务审批不通过
    REJECTED("2", "被驳回", true),
    REVOKED("3", "已撤销", true),
    COMPLETED("4", "已完成", false);



    private final String value;
    /**
     * 描述
     */
    private final String desc;

    private final boolean state;
}
