package com.zhaocai.business.integration.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.zhaocai.business.integration.dto.IntegrationCallLogCommand;
import com.zhaocai.business.integration.service.IIntegrationCallLogService;
import com.zhaocai.business.vendor.domain.TInterfaceLog;
import com.zhaocai.business.vendor.service.ITInterfaceLogService;
import org.springframework.stereotype.Service;

/** 使用历史表持久化的集成域适配器，后续可无感替换表结构。 */
@Service
public class IntegrationCallLogServiceImpl implements IIntegrationCallLogService {
    private final ITInterfaceLogService legacyService;

    public IntegrationCallLogServiceImpl(ITInterfaceLogService legacyService) {
        this.legacyService = legacyService;
    }

    @Override
    public void record(IntegrationCallLogCommand command) {
        TInterfaceLog log = new TInterfaceLog();
        log.setId(IdUtil.getSnowflake(1, 1).nextId());
        log.setSendTime(command.getSendTime());
        log.setSendData(command.getSendData());
        log.setSendUrl(command.getSendUrl());
        log.setReceiveData(command.getReceiveData());
        log.setReceiveTime(command.getReceiveTime());
        log.setLogType(command.getLogType());
        log.setFlag(Boolean.toString(command.isSuccessful()));
        log.setBusinessId(command.getBusinessId());
        log.setRemark(command.getRemark());
        legacyService.insertTInterfaceLog(log);
    }

    @Override
    public boolean hasSuccessfulRecord(String businessId) {
        TInterfaceLog query = new TInterfaceLog();
        query.setBusinessId(businessId);
        query.setFlag(Boolean.TRUE.toString());
        return CollectionUtil.isNotEmpty(legacyService.selectTInterfaceLogList(query));
    }
}
