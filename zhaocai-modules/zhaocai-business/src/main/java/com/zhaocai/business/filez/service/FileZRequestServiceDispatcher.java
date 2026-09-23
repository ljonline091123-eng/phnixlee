package com.zhaocai.business.filez.service;


import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.impl.ApplyWatermarkService;
import com.zhaocai.business.filez.service.impl.ConvertService;
import com.zhaocai.business.filez.service.impl.FileZDownloadService;
import com.zhaocai.business.filez.service.impl.UpdateBookmarkRefService;
import com.zhaocai.common.core.utils.SpringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 联想文件请求调度器
 *
 * @author chenming
 * @date 2024-07-24
 */
public class FileZRequestServiceDispatcher {
    private static Map<String,FileZRequestService> requestServiceMap;

    private FileZRequestServiceDispatcher(){
        requestServiceMap = new HashMap<>();

        requestServiceMap.put(FileZRequestTypeEnum.APPLY_WATERMARK.getRequestCode(), SpringUtils.getBean(ApplyWatermarkService.class));
        requestServiceMap.put(FileZRequestTypeEnum.DOWNLOAD.getRequestCode(), SpringUtils.getBean(FileZDownloadService.class));
        requestServiceMap.put(FileZRequestTypeEnum.UPDATE_BOOKMARK_REF.getRequestCode(), SpringUtils.getBean(UpdateBookmarkRefService.class));
        requestServiceMap.put(FileZRequestTypeEnum.CONVERT.getRequestCode(), SpringUtils.getBean(ConvertService.class));
    }

    private static class FileZRequestServiceDispatcherHandler {
        private static final FileZRequestServiceDispatcher INSTANCE = new FileZRequestServiceDispatcher();
    }

    public static FileZRequestServiceDispatcher getInstance(){
        return FileZRequestServiceDispatcherHandler.INSTANCE;
    }

    /**
     * 发送请求
     * @param requestContext
     */
    public void exchange(FileZRequestContext requestContext) {
        FileZRequestTypeEnum requestType = requestContext.getRequestTypeEnum();
        FileZRequestService requestService = requestServiceMap.get(requestType.getRequestCode());
        if (requestService == null) {
            throw new BusinessException("requestCode["+requestType.getRequestCode()+"]对应的实现类不存在，请确认");
        }

        requestService.sendFileZRequest(requestContext);
    }
}
