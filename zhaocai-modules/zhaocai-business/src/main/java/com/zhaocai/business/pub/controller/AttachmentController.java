package com.zhaocai.business.pub.controller;

import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.agreement.vo.res.AgreementBookmarkVO;
import com.zhaocai.business.agreement.vo.res.AgreementDetailVO;
import com.zhaocai.business.agreement.vo.res.AgreementVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.config.FileYOZOConfig;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.demo.app.DemoTestFile;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.utils.BookmarkUtils;
import com.zhaocai.business.pub.utils.Sender;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.AttachmentIdRequest;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.req.FileBeanVo;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.pub.vo.res.DictListVO;
import com.zhaocai.business.sdk.bean.ConvertParams;
import com.zhaocai.business.sdk.bean.EditParams;
import com.zhaocai.business.sdk.bean.PreviewParams;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.zhaocai.business.sdk.bean.BookMark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;




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

    @Autowired
    private YOZOfileUtils yozOfileUtils;

    @Autowired
    private BookmarkUtils bookmarkUtils;

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private Sender sender;



    @PostMapping("/addAttachment")
    @ApiModelProperty(value = "保存附件信息")
    public ResultData<Long> addAttachment(@RequestBody AttachmentRequestVO requestVO) {
        return ResultData.data(attachmentService.saveAttachment(requestVO));
    }

//    @GetMapping("/test1")
//    public ResultData<String> test(@RequestParam("fileName") String fileName) {
////        招标文件-材料_2190218303_20240918151504A38_20241217091602634.docx
//        String filename = yozOfileUtils.modifyFileName(fileName);
//        return ResultData.data(filename);
////        String bookmarkLabel = bookmarkUtils.getBookmarkLabel();
////        return ResultData.data(bookmarkLabel);
//    }
//
//    @GetMapping("/test2")
//    public ResultData<String> test2() {
//        String fileURl = "http://192.168.240.21:9000/wh-hnjt/招标文件-材料_20240714162504A030_9a1fb7ab8f7b4f21946192362242f444_c721982f4c904a06809588a00192d07a.docx";
//        String fileName = "招标文件-材料_20240714162504A030_9a1fb7ab8f7b4f21946192362242f444_c721982f4c904a06809588a00192d07a.docx";
//        Long agreementId = 1845013355260067842L;
//        AgreementDetailVO agreementDetailVO = agreementService.detail(agreementId);
//        AgreementVO agreementVO = agreementDetailVO.getAgreement();
//        AgreementBookmarkVO agreementBookmarkVO = BeanCopierUtil.copyBean(agreementVO,AgreementBookmarkVO.class);
//        String RentalMethodText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_RENTAL_METHOD.getName(),agreementBookmarkVO.getRentalMethod().toString());
//        agreementBookmarkVO.setRentalMethodText(RentalMethodText);
//        String fileURL= bookmarkUtils.FillBookmarkData(fileURl,fileName,agreementBookmarkVO);
////        Path path = yozOfileUtils.downloadFile(fileURl, yozOfileUtils.createTempFilePath(fileName));
////        System.out.println("下载地址：" + path.toString());
//        return ResultData.data(fileURL);
//    }

    /*
     * 复制模板文件时（修改文件名和文件URL）
     * */
    @PostMapping("/ModifyFileNameAndFileURL")
    @ApiModelProperty(value = "修改文件名和文件URL")
    public ResultData<String> ModifyFileNameAndFileURL(@RequestBody AttachmentIdRequest request) throws IOException {
        Long attachmentId = request.getAttachmentId();
        attachmentService.ModifyFileNameAndFileURL(attachmentId);
        return ResultData.success();
    }

    /*
    * 预览文件-据文件名和文件URL
    * */
    @GetMapping("/getViweFileURL")
    @ApiModelProperty(value = "获取预览附件url")
    public ResultData<String> getViweFileURL(AttachmentRequestVO requestVO) {
        String fileName = requestVO.getFileName();
        String fileUrl = requestVO.getFileUrl();
        if(StringUtils.isEmpty(fileUrl) || StringUtils.isEmpty(fileName)){
            return ResultData.fail("该文件存储的fileUrl或者fileName为空，无法预览文件！！！");
        }
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        if (yozOfileUtils.isWordExtension(suffix)) {
            String viewURL = attachmentService.viewWordFileURL(fileName,fileUrl);
            return ResultData.data(viewURL);
        } else if(yozOfileUtils.isPdfExtension(suffix)){
            return ResultData.data(attachmentService.viewPDFFileURL(fileName,fileUrl));
        } else if (yozOfileUtils.isImageExtension(suffix)) {
            return ResultData.data(attachmentService.viewImageURL(fileName,fileUrl));
        }else {
            return ResultData.fail("文件格式错误！！无法预览该格式的文件！");
        }
    }


    @GetMapping("/getViweFileUrlByID")
    @ApiModelProperty(value = "据attachmentId获取预览附件url")
    public ResultData<String> getViweFileUrlByID(@RequestParam("attachmentId") Long attachmentId) {
        if(attachmentId == null){
            return ResultData.fail("生成文件预览url失败,attachmentId为空，请检查！");
        }
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法预览文件！！！");
        }
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        if (yozOfileUtils.isWordExtension(suffix)) {
            return ResultData.data(attachmentService.viewWordFileURL(fileName,fileUrl));
        } else if(yozOfileUtils.isPdfExtension(suffix)){
            return ResultData.data(attachmentService.viewPDFFileURL(fileName,fileUrl));
        } else if (yozOfileUtils.isImageExtension(suffix)) {
            return ResultData.data(attachmentService.viewImageURL(fileName,fileUrl));
        }else {
            return ResultData.fail("无法预览该文件格式！");
        }
    }

    //据attachmentId查询附件，获取附件的文档中台的编辑URL
    @GetMapping("/getEditFileUrlByID")
    @ApiModelProperty(value = "据attachmentId获取编辑附件url")
    public ResultData<String> getEditFileUrlByID(@RequestParam("attachmentId") Long attachmentId) {
        if(attachmentId == null){
            return ResultData.fail("生成文件编辑url失败,attachmentId为空，请检查！");
        }
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法编辑文件！！！");
        }
        return ResultData.data(attachmentService.editWordURL(attachmentId,fileName,fileUrl));
    }


    @PostMapping("/fileUpload")
    @ApiOperation(value = "中台文件上传至minio")
    public String  fileUpload(@RequestBody FileBeanVo fileBean) throws IOException {
        System.out.println("上传文件");
        //读取文件
        Path path =  yozOfileUtils.downloadFile(fileBean.getFileUrl(),yozOfileUtils.createTempFilePath(fileBean.getFilename()));
        File file = new File(path.toString());
        if (!file.exists()) {
            throw new IOException("文档不存在: " + fileBean.getFileUrl());
        }
        Long fileId = fileBean.getFileId()==null?null:Long.parseLong(fileBean.getFileId());
        System.out.println("文件ID:"+fileId);
        Attachment attcha =  attachmentService.getById(fileId);
        if(attcha ==null){
            throw new IOException("文档不存在: " + fileBean.getFileUrl());
        }
        // 转换为InputStream
        FileInputStream fis = new FileInputStream(file);
//        String NewFileName = yozOfileUtils.modifyFileName(fileBean.getFilename());
        String fileUrl = sysFileService.uploadFile(fis, fileBean.getFilename());
        attcha.setFileUrl(fileUrl);
        attachmentService.updateBusiness(attcha.getId(),attcha.getBusinessId(),fileUrl,fileBean.getFilename());
        System.out.println("fileUrl:"+fileUrl);
        return fileUrl;
    }
}
