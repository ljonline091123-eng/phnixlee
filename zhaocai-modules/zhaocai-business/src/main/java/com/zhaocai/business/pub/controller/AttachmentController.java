package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.config.FileYOZOConfig;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.utils.Sender;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.req.FileBeanVo;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.sdk.bean.EditParams;
import com.zhaocai.business.sdk.bean.PreviewParams;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private YOZOfileUtils yozOfileUtils;

    @Autowired
    private FileYOZOConfig  fileYOZOConfig;


    @PostMapping("/addAttachment")
    @ApiModelProperty(value = "保存附件信息")
    public ResultData<Long> addAttachment(@RequestBody AttachmentRequestVO requestVO) {
        return ResultData.data(attachmentService.saveAttachment(requestVO));
    }



    @GetMapping("/getViweFileURL")
    @ApiModelProperty(value = "获取预览附件url")
    public ResultData<String> getViweFileURL(AttachmentRequestVO requestVO) {
        String fileName = requestVO.getFileName();
        String fileUrl = requestVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法预览文件！！！");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        String response;
        ResultData<String> result = null;
        // 组织请求参数
        PreviewParams params = new PreviewParams();
        try {
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            if (yozOfileUtils.isWordExtension(suffix)) {
                // 是否显示修订
                params.setAcceptTracks(false);
                // 允许复制
                params.setCopy(false);
                // 只允许打开一次
                params.setPreviewNumber(5);
                response= Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);
            } else if(yozOfileUtils.isPdfExtension(suffix)){
                // 允许复制
                params.setCopy(true);
                response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
                System.out.println("预览pdf文件响应结果：");
                System.out.println(response);
//                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
//                System.out.println(viewUrl);
            } else if (yozOfileUtils.isImageExtension(suffix)) {
                response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
                System.out.println("预览图片文件响应结果：");
                System.out.println(response);
//                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
//                System.out.println(viewUrl);
            }else {
                return ResultData.fail("无法预览该文件格式！");
            }
            String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
            String newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
            System.out.println(newViewUrl);
            result = ResultData.data(newViewUrl);
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败", e);
        }
        if (result == null) {
            result = ResultData.fail("生成文件预览url失败！");
        }
        //删除生成的临时文件
        System.out.println("删除文件路径:" + path.toString());
        yozOfileUtils.deleteTempFilePath(path.toString());
        return result;
    }


    @GetMapping("/getViweFileUrlByID")
    @ApiModelProperty(value = "据attachmentId获取预览附件url")
    public ResultData<String> getViweFileUrlByID(@RequestParam("attachmentId") Long attachmentId) {
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法预览文件！！！");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        String response;
        ResultData<String> result = null;
        // 组织请求参数
        PreviewParams params = new PreviewParams();
        try {
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            if (yozOfileUtils.isWordExtension(suffix)) {
                // 是否显示修订
                params.setAcceptTracks(false);
                // 允许复制
                params.setCopy(false);
                // 只允许打开一次
                params.setPreviewNumber(5);
                response= Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);
            } else if(yozOfileUtils.isPdfExtension(suffix)){
                // 允许复制
                params.setCopy(true);
                response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
                System.out.println("预览pdf文件响应结果：");
                System.out.println(response);
                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                System.out.println(viewUrl);
            } else if (yozOfileUtils.isImageExtension(suffix)) {
                response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
                System.out.println("预览图片文件响应结果：");
                System.out.println(response);
                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                System.out.println(viewUrl);
            }else {
                return ResultData.fail("无法预览该文件格式！");
            }
            String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
            String newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
            System.out.println(newViewUrl);
            result = ResultData.data(newViewUrl);
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败", e);
        }
        if (result == null) {
            result = ResultData.fail("生成文件预览url失败！");
        }
        //删除生成的临时文件
        System.out.println("删除文件路径:" + path.toString());
        yozOfileUtils.deleteTempFilePath(path.toString());
        return result;
    }

    //据attachmentId查询附件，获取附件的文档中台的编辑URL
    @GetMapping("/getEditFileUrlByID")
    @ApiModelProperty(value = "据attachmentId获取编辑附件url")
    public ResultData<String> getEditFileUrlByID(@RequestParam("attachmentId") Long attachmentId) {
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法编辑文件！！！");
        }
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        //当前登录用户信息
        Long loginUserId = SecurityUtils.getUserId();
        String loginUserName = SecurityUtils.getUsername();
        try {
            // 编辑文档的-组织请求参数
            EditParams params = new EditParams();
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setUserInfo(loginUserId.toString(), loginUserName);
            params.setUserRight(EditParams.USERRIGHT_EDIT);
            params.setFileUUID(attachmentId.toString());
            // 自动保存
            params.setSaveFlag(true);
            // 回调地址支持2中方式获取文件，请根据需要按照接口规范实现接口
            params.setCallbackUrl(fileYOZOConfig.getCallbackUrl());
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, "");
            // 设置文档显示比例，不设置则按照文档中保存的比例显示
            params.setPageZoom(100);
            // 开档打开修订
//		params.trackRevisionsOpen();
            // 开档关闭修订
            params.trackRevisionsClose();
            // 显示修订记录
            params.trackRevisionsShow();
            // 隐藏修订记录
            // params.trackRevisionsHidden();
            params.trackRevisionsAcceptRejectEnable();
            // 清稿（修订记录全部接受）
            // params.trackRevisionsClear();
            // 设置复制粘贴剪切是否可用
            params.setCopyPasteState(true, true, false);
            // 设置书签时可选择的内容，暂未实现
          //  params.setBookMarkListRange("甲方,乙方,金额,签订日期");

            String response = Sender.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
            System.out.println("编辑响应结果：");
            System.out.println(response);
            String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
            String newEditUrl = yozOfileUtils.updateFileUrl(editUrl);
            System.out.println(newEditUrl);
            return ResultData.data(newEditUrl);
        } catch (Exception e) {
            throw new RuntimeException("生成文件编辑url失败", e);
        }
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
        String fileUrl = sysFileService.uploadFile(fis, fileBean.getFilename());
        attcha.setFileUrl(fileUrl);
        attachmentService.updateBusiness(attcha.getId(),attcha.getBusinessId(),fileUrl,fileBean.getFilename());
        System.out.println("fileUrl:"+fileUrl);
        return fileUrl;
    }
}
