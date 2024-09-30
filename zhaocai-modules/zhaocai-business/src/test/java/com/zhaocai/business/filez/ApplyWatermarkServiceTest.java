package com.zhaocai.business.filez;

import com.zhaocai.business.base.SpringBaseTest;
import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.filez.dto.WatermarkRequestDTO;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.impl.ApplyWatermarkService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplyWatermarkServiceTest extends SpringBaseTest {

    @Autowired
    private ApplyWatermarkService applyWatermarkService;

    @Test
    public void applyWaterTest() {
        FileZRequestContext requestContext = new FileZRequestContext(FileZRequestTypeEnum.APPLY_WATERMARK,FileZTaskBusinessEnum.AGREEMENT_APPLY_WATERMARK, 1813820095085907969L);
        WatermarkRequestDTO requestDTO = new WatermarkRequestDTO();
        requestDTO.setText("湖南建投-一分公司");
        requestDTO.setFileUrl("https://zhaocai-minio-dev.wanheng.tech/wh-hnjt/2024/07/18/content_20240718144651A002.docx");
        requestContext.setFileZRequest(requestDTO);

        applyWatermarkService.sendFileZRequest(requestContext);
    }
}
