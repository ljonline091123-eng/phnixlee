package com.zhaocai.business.bidding.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;


@Getter
@AllArgsConstructor
public enum TenderNoticeApprovalStatusEnum {

    DRAFT(0,"自由态"),/* 保存时状态 */
    IN_APPROVAL(1,"审批中"),/* 驳回到中途节点也是审批中 */
    CANCELLATION(2,"已作废"),
    APPROVE(3,"已完成"),/* 审批通过后 */
    REJECT(4,"已驳回"),/* 驳回到发起人才是已驳回 */
    REVOKED(5,"已撤回"),/* 发起人撤回？ */
    ;

    private final Integer state;

    private final String desc;

    /**
     * 根据编码查询枚举
     *
     * @param state
     * @return
     */
    public static String getValueByCode(Integer state) {
        if (Objects.isNull(state)) {
            return null;
        }
        for (TenderNoticeApprovalStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }

}
