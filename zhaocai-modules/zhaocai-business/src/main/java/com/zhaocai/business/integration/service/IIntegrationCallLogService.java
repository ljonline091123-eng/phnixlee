package com.zhaocai.business.integration.service;

import com.zhaocai.business.integration.dto.IntegrationCallLogCommand;

public interface IIntegrationCallLogService {
    void record(IntegrationCallLogCommand command);

    boolean hasSuccessfulRecord(String businessId);
}
