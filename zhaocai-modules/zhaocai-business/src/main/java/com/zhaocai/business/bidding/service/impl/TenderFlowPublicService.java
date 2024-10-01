package com.zhaocai.business.bidding.service.impl;

import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

/**
 * @description 公开招标流程
 * @author ssy
 * @date 2024/6/30 14:36
 */
@Service
public class TenderFlowPublicService extends TenderFlowService {



    public static LinkedList<Integer> statusEnumList = new LinkedList<Integer>(){{
        add(0, TenderNoticeStatusEnum.TENDER_NOTICE.getState());
        add(1, TenderNoticeStatusEnum.TENDER_REGISTER.getState());
        add(2, TenderNoticeStatusEnum.TENDER_REGISTER_SAVE.getState());
        add(3, TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        add(4, TenderNoticeStatusEnum.BID_OPENING.getState());
        add(5, TenderNoticeStatusEnum.EVALUATION_BID.getState());
        add(6, TenderNoticeStatusEnum.CALI_REPORT.getState());
        add(7, TenderNoticeStatusEnum.WINNING_BID.getState());
        add(8, TenderNoticeStatusEnum.RESULT_RELEASE.getState());
        add(9, TenderNoticeStatusEnum.COMPLETE.getState());
    }};

}
