package com.zhaocai.business.filez.service.impl;

import com.zhaocai.business.filez.dto.WatermarkRequestDTO;
import com.zhaocai.business.filez.service.AbstractDocumentUpdateService;
import com.zhaocai.business.filez.service.dto.FileZRequestBaseOps;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.WatermarkRequestOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 加水印
 */
@Service
@Slf4j
public class ApplyWatermarkService extends AbstractDocumentUpdateService {

    @Override
    protected List<FileZRequestBaseOps> buildOps(FileZRequestContext requestContext) {
        WatermarkRequestDTO requestDTO = (WatermarkRequestDTO) requestContext.getFileZRequest();

        WatermarkRequestOps.WatermarkOptions watermarkOptions = new WatermarkRequestOps.WatermarkOptions(requestDTO.getText());

        List<FileZRequestBaseOps> opsList = new ArrayList<>();
        opsList.add(new WatermarkRequestOps(watermarkOptions));
        return opsList;
    }
}
