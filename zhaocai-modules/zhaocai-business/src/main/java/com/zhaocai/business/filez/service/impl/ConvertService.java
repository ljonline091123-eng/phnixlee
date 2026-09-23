package com.zhaocai.business.filez.service.impl;

import com.zhaocai.business.filez.dto.CovertRequestDTO;
import com.zhaocai.business.filez.service.AbstractFileZRequestService;
import com.zhaocai.business.filez.service.dto.ConvertRequestDTO;
import com.zhaocai.business.filez.service.dto.FileZRequest;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.TiledWatermark;
import com.zhaocai.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 格式转换
 *
 * @author chenming
 * @date 2024-07-30
 */
@Service
public class ConvertService extends AbstractFileZRequestService {

    @Override
    protected FileZRequest buildRequestParams(FileZRequestContext requestContext) {
        CovertRequestDTO convertDto = (CovertRequestDTO) requestContext.getFileZRequest();

        ConvertRequestDTO requestDTO = new ConvertRequestDTO(convertDto.getFileUrl());
        requestDTO.setTargetFilename(convertDto.getTargetFileName() + "." +convertDto.getTargetFileType());
        requestDTO.setTokenType("cookie");
        requestDTO.setTokenValue("fileZTaskId=" + requestContext.getFileZTaskId());
        requestDTO.setCallback(getCallback());
        requestDTO.setUniqueId(requestContext.getFileZTaskId().toString());

        if (StringUtils.isNotBlank(convertDto.getWatermarkText())) {
            TiledWatermark watermark = new TiledWatermark(convertDto.getWatermarkText());
            requestDTO.setTiledWatermark(watermark);
        }

        return requestDTO;
    }
}
