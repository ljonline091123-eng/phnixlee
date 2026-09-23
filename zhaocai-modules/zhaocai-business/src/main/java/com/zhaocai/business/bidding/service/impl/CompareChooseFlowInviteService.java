package com.zhaocai.business.bidding.service.impl;

import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;

import java.util.LinkedList;

/**
 * @description 邀请招标流程
 * @author ssy
 * @date 2024/7/2 15:01
 */
public class CompareChooseFlowInviteService extends TenderFlowService {

    public static LinkedList<Integer> statusEnumList = new LinkedList<Integer>(){{
        add(0, TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        add(1, TenderNoticeStatusEnum.BID_OPENING.getState());
        add(2, TenderNoticeStatusEnum.EVALUATION_BID.getState());
        add(3, TenderNoticeStatusEnum.CALI_REPORT.getState());
        add(4, TenderNoticeStatusEnum.RESULT_RELEASE.getState());
        add(5, TenderNoticeStatusEnum.COMPLETE.getState());
    }};

}
