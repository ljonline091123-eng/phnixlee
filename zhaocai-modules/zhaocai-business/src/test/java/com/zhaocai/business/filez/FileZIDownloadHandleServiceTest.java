package com.zhaocai.business.filez;

import com.zhaocai.business.base.SpringBaseTest;
import com.zhaocai.business.common.enums.FileZRequestTypeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.filez.dto.FileZDownloadRequestDTO;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.impl.FileZDownloadService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FileZIDownloadHandleServiceTest extends SpringBaseTest {

    @Autowired
    private FileZDownloadService fileZDownloadService;

    @Test
    public void downloadTest() {
        FileZRequestContext requestContext = new FileZRequestContext(FileZRequestTypeEnum.DOWNLOAD,FileZTaskBusinessEnum.AGREEMENT_CONVERT_TO_PDF, 1813820095085907969L);

        FileZDownloadRequestDTO requestDTO = new FileZDownloadRequestDTO("c46d1b91-1830-4c12-a477-313fd056c06d-111","66a0ab1e5e02a3da101af5cf-22","vdp_20240722163140A119.docx");
        requestContext.setFileZRequest(requestDTO);

        fileZDownloadService.sendFileZRequest(requestContext);
    }
}
