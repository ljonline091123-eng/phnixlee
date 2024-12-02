package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.fileYOZO.util.fileUtils;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.req.FileBeanVo;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;


/**
 * 附件
 *
 * @author chenming
 * @date 2024-06-26
 */
@Api(value = "附件")
@RestController
@RequestMapping("/attachment")
public class AttachmentController extends BladeController {

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private ISysFileService sysFileService;


    @PostMapping("/addAttachment")
    @ApiModelProperty(value = "保存附件信息")
    public ResultData<Long> addAttachment(@RequestBody AttachmentRequestVO requestVO) {
        return ResultData.data(attachmentService.saveAttachment(requestVO));
    }

    @PostMapping("/fileUpload")
    @ApiOperation(value = "中台文件上传至minio")
    public String  fileUpload(@RequestBody FileBeanVo fileBean) throws IOException {
        //读取文件
        Path path =  fileUtils.downloadFile(fileBean.getFileUrl(),fileUtils.createTempFilePath(fileBean.getFilename()));
        File file = new File(path.toString());
        if (!file.exists()) {
            throw new IOException("文档不存在: " + fileBean.getFileUrl());
        }
        Long fileId = fileBean.getFileId()==null?null:Long.parseLong(fileBean.getFileId());
        Attachment attcha =  attachmentService.getById(fileId);
        // 转换为InputStream
        FileInputStream fis = new FileInputStream(file);
        String fileUrl = sysFileService.uploadFile(fis, fileBean.getFilename());
        attcha.setFileUrl(fileUrl);
        attachmentService.save(attcha);
        return fileUrl;
    }
}
