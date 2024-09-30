package com.zhaocai.business.bidding.service.impl;

import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;

import java.util.LinkedList;

/**
 * @description 单一来源招标流程
 * @author ssy
 * @date 2024/7/2 15:06
 */
public class TenderFlowSingleService extends TenderFlowService {

    public static LinkedList<Integer> statusEnumList = new LinkedList<Integer>(){{
        add(0, TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        add(1, TenderNoticeStatusEnum.CALI_REPORT.getState());
        add(2, TenderNoticeStatusEnum.RESULT_RELEASE.getState());
        add(3, TenderNoticeStatusEnum.COMPLETE.getState());
    }};

}
