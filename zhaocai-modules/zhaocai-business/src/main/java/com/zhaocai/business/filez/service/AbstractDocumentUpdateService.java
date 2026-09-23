package com.zhaocai.business.filez.service;

import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.filez.dto.DocumentUpdateRequestDTO;
import com.zhaocai.business.filez.service.dto.DocumentUpdateRequest;
import com.zhaocai.business.filez.service.dto.FileZRequest;
import com.zhaocai.business.filez.service.dto.FileZRequestBaseOps;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.file.FileUtils;

import java.util.List;


/**
 * 文档操作类服务
 *
 * @author chenming
 * @date 2024-07-05
 */
public abstract class AbstractDocumentUpdateService extends AbstractFileZRequestService {

    @Override
    protected FileZRequest buildRequestParams(FileZRequestContext requestContext) {
        DocumentUpdateRequest request = new DocumentUpdateRequest();
        request.setFileUrl(getFileUrl(requestContext));
        request.setFilename(getFileName(requestContext));
        request.setTokenType("cookie");
        request.setTokenValue(getTokenValue(requestContext));
        request.setCallback(getCallback());
        request.setOps(buildOps(requestContext));

        return request;
    }

    /**
     * 获取 tokenValue，格式为key=value;key2=value2
     * @return
     */
    private String getTokenValue(FileZRequestContext requestContext) {
        return "fileZTaskId=" + requestContext.getFileZTaskId() + ";operateType=" + requestContext.getRequestTypeEnum().getRequestCode();
    }

    /**
     * 获取文件 url
     * @return
     */
    private String getFileUrl(FileZRequestContext requestContext) {
        DocumentUpdateRequestDTO requestDTO = (DocumentUpdateRequestDTO) requestContext.getFileZRequest();
        if (StringUtils.isBlank(requestDTO.getFileUrl())) {
            throw new ParamValidateException("操作的文件 url 不能为空");
        }

        // 替换地址，因为联想文档需要文件地址和服务器地址的 hostname 是一致的
        String minioUrl = EnvironmentUtil.getProperty("file-z.minio-url");
        String minioUrlReplace = EnvironmentUtil.getProperty("file-z.minio-url-replace");
        return requestDTO.getFileUrl().replace(minioUrl,minioUrlReplace);
    }

    /**
     * 获取文件名
     * @param requestContext
     * @return
     */
    private String getFileName(FileZRequestContext requestContext) {
        DocumentUpdateRequestDTO requestDTO = (DocumentUpdateRequestDTO) requestContext.getFileZRequest();
        if (StringUtils.isBlank(requestDTO.getFileName())) {
            throw new ParamValidateException("操作的文件名不能为空");
        }
        return requestDTO.getFileName();
    }

    /**
     * 构建操作的有序数据
     * @param requestContext
     * @return
     */
    protected abstract List<FileZRequestBaseOps> buildOps(FileZRequestContext requestContext);
}
