package com.zhaocai.business.pub.service.impl;

import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.IDownloadHandleService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文件下载处理服务
 *
 * @author chenming
 * @date 2024-07-24
 */
@Service
public class IDownloadHandleServiceImpl implements IDownloadHandleService {

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private IAttachmentService attachmentService;

    @Override
    public Long downloadHandle(byte[] bytes, AttachmentTypeEnum businessType, long businessId,String fileName) {
        // 上传文件
        InputStream inputStream = new ByteArrayInputStream(bytes);
        String fileUrl = sysFileService.uploadFile(inputStream,fileName);

        AttachmentRequestVO requestVO = new AttachmentRequestVO();
        requestVO.setFileName(fileName);
        requestVO.setFileUrl(fileUrl);

        return attachmentService.addAttachment(requestVO,businessType,businessId);
    }
}
