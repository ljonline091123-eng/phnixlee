package com.zhaocai.business.filez.service.impl;

import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.FileZResponseCodeEnum;
import com.zhaocai.business.filez.dto.FileZDownloadRequestDTO;
import com.zhaocai.business.filez.dto.FileZDownloadResponseDTO;
import com.zhaocai.business.filez.service.AbstractFileZRequestService;
import com.zhaocai.business.filez.service.dto.FileZDownloadRequest;
import com.zhaocai.business.filez.service.dto.FileZRequest;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.FileZResponse;
import com.zhaocai.business.pub.service.IDownloadHandleService;
import com.zhaocai.common.core.utils.JacksonUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 联想文档文件下载服务
 *
 * @author chenming
 * @date 2024-07-24
 */
@Slf4j
@Service
public class FileZDownloadService extends AbstractFileZRequestService {

    @Autowired
    private IDownloadHandleService downloadHandleService;

    @Override
    protected FileZRequest buildRequestParams(FileZRequestContext requestContext) {
        FileZDownloadRequestDTO requestDTO = (FileZDownloadRequestDTO) requestContext.getFileZRequest();

        return BeanCopierUtil.copyBean(requestDTO, FileZDownloadRequest.class);
    }

    @Override
    protected void responseHandler(long fileTaskId, Object responseBody,FileZRequestContext requestContext) {
        String responseStr = null;
        if (responseBody instanceof byte[]) {
            // 如果响应类型为文件流，则表示文件下载已成功
            byte[] bytes = (byte[]) responseBody;
            FileZDownloadRequestDTO requestDTO = (FileZDownloadRequestDTO) requestContext.getFileZRequest();

            long attachmentId = downloadHandleService.downloadHandle(bytes,AttachmentTypeEnum.FILE_Z_DOWNLOAD,fileTaskId,requestDTO.getFileName());

            // 设置处理结果
            FileZDownloadResponseDTO responseDTO = new FileZDownloadResponseDTO(attachmentId);
            requestContext.setFileZResponse(responseDTO);

            // 构造响应结果
            FileZResponse response = new FileZResponse();
            response.setCode(FileZResponseCodeEnum.OK.getCode());
            responseStr = JacksonUtil.toJsonString(response);
        } else {
            // 不是文件流，表示文件下载以失败，转换为 String
            responseStr = responseBody == null ? "" : responseBody.toString();
        }

        // 回写响应结果
        super.responseHandler(fileTaskId,responseStr,requestContext);
    }
}
