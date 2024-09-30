package com.zhaocai.business.filez.service;

import com.zhaocai.business.filez.service.dto.FileZRequestContext;

/**
 * FileZ 服务接口
 *
 * @author chenming
 * @date 2024-07-05
 */
public interface FileZRequestService {

    void sendFileZRequest(FileZRequestContext fileZRequestContext);
}
