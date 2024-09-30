package com.zhaocai.flowable.listener;

import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.flowable.mapper.TenderNoticeMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * @author ssy
 * @date 2024/6/23 14:16
 */
public class TenderNoticeExecutionListener implements ExecutionListener {

    private TenderNoticeMapper tenderNoticeMapper = SpringUtils.getBean(TenderNoticeMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) {
        Long tenderNoticeId = Long.valueOf(delegateExecution.getVariable("tenderNoticeId").toString()) ;
        Integer nextNoticeStatus = Integer.valueOf(delegateExecution.getVariable("noticeStatus").toString()) ;
        tenderNoticeMapper.updateTenderNoticeStatus(tenderNoticeId, nextNoticeStatus);
    }

}
