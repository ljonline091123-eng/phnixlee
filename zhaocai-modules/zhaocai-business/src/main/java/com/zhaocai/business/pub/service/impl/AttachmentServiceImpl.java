package com.zhaocai.business.pub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.config.FileYOZOConfig;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.exception.NotFoundException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.mapper.AttachmentMapper;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.utils.BookmarkUtils;
import com.zhaocai.business.pub.utils.Sender;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.sdk.bean.ConvertParams;
import com.zhaocai.business.sdk.bean.EditParams;
import com.zhaocai.business.sdk.bean.PreviewParams;
import com.zhaocai.business.sdk.bean.WaterMark;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Path;

/**
 * 附件Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements IAttachmentService {

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private YOZOfileUtils yozOfileUtils;

    @Autowired
    private FileYOZOConfig fileYOZOConfig;

    @Autowired
    private BookmarkUtils bookmarkUtils;

    @Autowired
    private Sender sender;

    //修改附件的文件名和文件URL
    @Override
    public void  ModifyFileNameAndFileURL(Long attachmentId) throws IOException {
        if(attachmentId == null){
            throw new NotFoundException("修改文件名失败，attachmentId为空，请检查！");
        }
        AttachmentVO attachmentVO = getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(StringUtils.isEmpty(fileUrl) || StringUtils.isEmpty(fileName)){
            throw new NotFoundException("修改文件名失败，该附件存储的fileUrl或者fileName为空！");
        }
        String NewFileName = yozOfileUtils.modifyFileName(fileName);
        //读取原文件，下载到临时文件夹
        Path path =  yozOfileUtils.downloadFile(fileUrl,yozOfileUtils.createTempFilePath(fileName));
        File file = new File(path.toString());
        if (!file.exists()) {
            throw new IOException("文档不存在: " + fileUrl);
        }
        // 转换为InputStream，上传到文档中台
        FileInputStream fis = new FileInputStream(file);
        String NewFileUrl = sysFileService.uploadFile(fis, NewFileName);
        updateFileNameANDFileUrl(attachmentId,NewFileUrl,NewFileName);
        //删除生成的临时文件
        System.out.println("删除文件路径:" + path);
        yozOfileUtils.deleteTempFilePath(path.toString());
    }

    //文档中台——获取预览word文件URL
    @Override
    public String  viewWordFileURL(String fileName, String fileUrl){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件预览url失败,未获取到文件名或者文件URL！");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        try {
            // 组织请求参数
            PreviewParams params = new PreviewParams();
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            // 是否显示修订
            params.setAcceptTracks(false);
            // 允许复制
            params.setCopy(false);
            // 只允许打开一次
            params.setPreviewNumber(5);
            String newViewUrl = null;
            try {
                String response= sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
                System.out.println(newViewUrl);
            }  catch (JSONException e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            //删除生成的临时文件
            System.out.println("删除文件路径:" + path.toString());
            yozOfileUtils.deleteTempFilePath(path.toString());
            return newViewUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败"+ e.getMessage(), e);
        }

    }

    //文档中台——获取预览word文件URL+加上水印
    @Override
    public String  viewWordFileURLWithWaterMarK(String fileName, String fileUrl, String waterMarkContent){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件预览url失败,未获取到文件名或者文件URL！");
        }
        if(StringUtils.isEmpty(waterMarkContent)){
            throw new RuntimeException("生成文件预览url失败,未获取到水印内容");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        try {
            // 组织请求参数
            PreviewParams params = new PreviewParams();
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            // 是否显示修订
            params.setAcceptTracks(true);
            // 允许复制
            params.setCopy(false);
            // 只允许打开一次
            params.setPreviewNumber(5);
            // 设置水印
            WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, waterMarkContent);
            params.setWaterMark(wm);
            String newViewUrl = null;
            try {
                String response= sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
                System.out.println(newViewUrl);
            } catch (JSONException e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            //删除生成的临时文件
            System.out.println("删除文件路径:" + path.toString());
            yozOfileUtils.deleteTempFilePath(path.toString());
            return newViewUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败"+ e.getMessage(), e);
        }

    }

    //文档中台——获取预览PDF文件URL
    @Override
    public String  viewPDFFileURL(String fileName, String fileUrl){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件预览url失败,未获取到文件名或者文件URL！");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        try {
            // 组织请求参数
            PreviewParams params = new PreviewParams();
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            // 允许复制
            params.setCopy(true);
            String newViewUrl = null;
            try {
                String response = sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
                System.out.println(newViewUrl);
            }  catch (JSONException e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            //删除生成的临时文件
            System.out.println("删除文件路径:" + path.toString());
            yozOfileUtils.deleteTempFilePath(path.toString());
            return newViewUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败"+ e.getMessage(), e);
        }

    }

    //文档中台——根据文件URL获取预览PDF文件URL（不用下载在本地）
    @Override
    public String  previewPdfUrlByFileUrl(String fileName, String fileUrl){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件预览url失败,未获取到文件名或者文件URL！");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        try {
            // 组织请求参数
            PreviewParams params = new PreviewParams();
            // 设置要预览的文件
            params.setFileUrl(fileUrl);
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            // 允许复制
            params.setCopy(true);
            String newViewUrl = null;
            try {
                String response = sender.post(PreviewParams.URL_PREVIEW_URL, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBodyString());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
                System.out.println(newViewUrl);
            } catch (JSONException e) {
                throw new RuntimeException("文档中台-解析服务器响应内容失败: " + e.getMessage(), e);
            }
            return newViewUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败"+ e.getMessage(), e);
        }

    }

    //文档中台——获取预览图片URL
    @Override
    public String  viewImageURL(String fileName, String fileUrl){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件预览url失败,未获取到文件名或者文件URL！");
        }
        String HtmlName = yozOfileUtils.removeSuffix(fileName);
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        try {
            // 组织请求参数
            PreviewParams params = new PreviewParams();
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 是否可打印
            params.setPrintMenu(true, false);
            // 设置可下载
            params.setDownloadMenu(true, fileName);
            String newViewUrl = null;
            try {
                String response = sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
                System.out.println("预览图片文件响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                newViewUrl = yozOfileUtils.updateFileUrl(viewUrl);
                System.out.println(newViewUrl);
            }  catch (JSONException e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            //删除生成的临时文件
            System.out.println("删除文件路径:" + path.toString());
            yozOfileUtils.deleteTempFilePath(path.toString());
            return newViewUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败"+ e.getMessage(), e);
        }

    }

    //文档中台——获取编辑word文档的URL-（开启限制编辑按钮）
    @Override
    public String  editWordURLWithLimitEdit(Long attachmentId, String fileName, String fileUrl){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件编辑url失败,未获取到文件名或者文件URL！");
        }
        if(NumberUtil.isNullOrZero(attachmentId)){
            throw new RuntimeException("生成文件编辑url失败,未获取到附件ID-attachmentId！");
        }
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        //当前登录用户信息
        String loginUserName = SecurityUtils.getUsername();
        String nickName = SecurityUtils.getLoginUserNickName();
        try {
            // 编辑文档的-组织请求参数
            EditParams params = new EditParams();
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setUserInfo(loginUserName,nickName);
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
            params.trackRevisionsOpen();
            // 开档关闭修订
//            params.trackRevisionsClose();
            // 显示修订记录
            params.trackRevisionsShow();
            // 隐藏修订记录
            // params.trackRevisionsHidden();
            params.trackRevisionsAcceptRejectEnable();
            // 清稿（修订记录全部接受）
            // params.trackRevisionsClear();
            // 设置复制粘贴剪切是否可用
            params.setCopyPasteState(true, true, false);
            //  设置书签时可选择的内容
            params.setBookMarkListRange(bookmarkUtils.getBookmarkLabel(),true,false);

            String newEditUrl = null;
            try {
                String response = sender.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
                System.out.println("编辑响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                if (!jsonResponse.has("data") || !jsonResponse.getJSONObject("data").has("editUrl")) {
                    throw new RuntimeException("生成文件编辑url失败: 服务器响应格式不正确或缺少必要字段editUrl");
                }
                String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
                newEditUrl = yozOfileUtils.updateFileUrl(editUrl);
                System.out.println(newEditUrl);
            } catch (Exception e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            return newEditUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件编辑url失败，" + e.getMessage(), e);
        }
    }

    //文档中台——获取编辑word文档的URL-(关闭限制编辑)
    @Override
    public String  editWordURL(Long attachmentId, String fileName, String fileUrl){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件编辑url失败,未获取到文件名或者文件URL！");
        }
        if(NumberUtil.isNullOrZero(attachmentId)){
            throw new RuntimeException("生成文件编辑url失败,未获取到附件ID-attachmentId！");
        }
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        //当前登录用户信息
        String loginUserName = SecurityUtils.getUsername();
        String nickName = SecurityUtils.getLoginUserNickName();
        try {
            // 编辑文档的-组织请求参数
            EditParams params = new EditParams();
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setUserInfo(loginUserName,nickName);
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
            params.trackRevisionsOpen();
            // 开档关闭修订
//            params.trackRevisionsClose();
            // 显示修订记录
            params.trackRevisionsShow();
            // 隐藏修订记录
            // params.trackRevisionsHidden();
            params.trackRevisionsAcceptRejectEnable();
            // 清稿（修订记录全部接受）
            // params.trackRevisionsClear();

            //将菜单里面的限制编辑按钮隐藏掉
            params.hiddenLimitedit(true);
            // 设置复制粘贴剪切是否可用
            params.setCopyPasteState(true, true, false);
            //  设置书签时可选择的内容
            params.setBookMarkListRange(bookmarkUtils.getBookmarkLabel(),true,false);

            String newEditUrl = null;
            try {
                String response = sender.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
                System.out.println("编辑响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                if (!jsonResponse.has("data") || !jsonResponse.getJSONObject("data").has("editUrl")) {
                    throw new RuntimeException("生成文件编辑url失败: 服务器响应格式不正确或缺少必要字段editUrl");
                }
                String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
                newEditUrl = yozOfileUtils.updateFileUrl(editUrl);
                System.out.println(newEditUrl);
            }  catch (Exception e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            return newEditUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件编辑url失败，" + e.getMessage(), e);
        }
    }

    //文档中台——获取编辑word文档的URL+ 加水印 （关闭限制编辑按钮）
    @Override
    public String  editWordURLWithWaterMark(Long attachmentId, String fileName, String fileUrl,String waterMarkContent){
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件编辑url失败,未获取到文件名或者文件URL！");
        }
        if(StringUtils.isEmpty(waterMarkContent)){
            throw new RuntimeException("生成文件编辑url失败,未获取到水印内容！");
        }
        if(NumberUtil.isNullOrZero(attachmentId)){
            throw new RuntimeException("生成文件编辑url失败,未获取到附件ID-attachmentId！");
        }
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        //当前登录用户信息
        String loginUserName = SecurityUtils.getUsername();
        String nickName = SecurityUtils.getLoginUserNickName();
        try {
            // 编辑文档的-组织请求参数
            EditParams params = new EditParams();
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setUserInfo(loginUserName,nickName);
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
            params.trackRevisionsOpen();
            // 开档关闭修订
//            params.trackRevisionsClose();
            // 显示修订记录
            params.trackRevisionsShow();
            // 隐藏修订记录
            // params.trackRevisionsHidden();
            params.trackRevisionsAcceptRejectEnable();
            // 清稿（修订记录全部接受）
            // params.trackRevisionsClear();

            //将菜单里面的限制编辑按钮隐藏掉
            params.hiddenLimitedit(true);
            // 设置复制粘贴剪切是否可用
            params.setCopyPasteState(true, true, false);
            //  设置书签时可选择的内容
            params.setBookMarkListRange(bookmarkUtils.getBookmarkLabel(),true,false);
            // 设置水印
            WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, waterMarkContent);
            // 将水印设置到参数中
            params.setWaterMark(EditParams.WATER_MARK_TYPE_PAGE, wm);
            String newEditUrl = null;
            try {
                String response = sender.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
                System.out.println("编辑响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                if (!jsonResponse.has("data") || !jsonResponse.getJSONObject("data").has("editUrl")) {
                    throw new RuntimeException("生成文件编辑url失败: 服务器响应格式不正确或缺少必要字段editUrl");
                }
                String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
                newEditUrl = yozOfileUtils.updateFileUrl(editUrl);
                System.out.println(newEditUrl);
            } catch (Exception e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            return newEditUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成文件编辑url失败，" + e.getMessage(), e);
        }

    }

    //文档中台-office转PDF
    @Override
    public String  convertOfficeToPdf(String fileName, String fileUrl, String waterMarkContent) {
        if(StringUtils.isEmpty(fileName) || StringUtils.isEmpty(fileUrl)){
            throw new RuntimeException("生成文件编辑url失败,未获取到文件名或者文件URL！");
        }
        if(StringUtils.isEmpty(waterMarkContent)){
            throw new RuntimeException("生成文件编辑url失败,未获取到水印内容！");
        }
        Path path = yozOfileUtils.downloadFile(fileUrl, yozOfileUtils.createTempFilePath(fileName));
        try {
            ConvertParams params = new ConvertParams();
            // 设置要处理的文档模版
            params.setFilePath(path.toString());
            // 设置水印
            WaterMark wm = new WaterMark(WaterMark.TYPE_TXT, waterMarkContent);
            // 将水印设置到参数中
            params.setWaterMark(wm);
            String viewUrl = null;
            try {
                String response = sender.post(ConvertParams.URL_CONVERT, ConvertParams.CONVERT_TYPE_DOC_PDF, params.getRequestBody());
                System.out.println("转换文件响应结果：");
                System.out.println(response);

                //抛出服务器响应错误
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1); // 默认值-1表示未找到该字段或转换失败
                String msg = jsonResponse.optString("msg", "未知错误");
                // 判断 code 是否为 0，响应成功则code为0；
                if (code != 0) {
                    System.err.println("文档中台-服务器响应错误: " + msg);
                    throw new RuntimeException("文档中台-服务器响应错误: " + msg);
                }

                viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                System.out.println(viewUrl);
            }  catch (Exception e) {
                throw new RuntimeException("文档中台-解析服务器响应失败: " + e.getMessage(), e);
            }
            return viewUrl;
        } catch (Exception e) {
            throw new RuntimeException("office转PDF文件失败"+ e.getMessage(), e);
        }
    }

    @Override
    public void addAttachment(List<AttachmentRequestVO> attachmentList, AttachmentTypeEnum businessType, Long businessId) {
        if (CollectionUtil.isNotEmpty(attachmentList)) {
            //这里先删除附件
            List<Attachment> attachments = super.list(new LambdaQueryWrapper<Attachment>()
                    .eq(Attachment::getBusinessType, businessType.getType())
                    .eq(Attachment::getBusinessId, businessId)
                    .eq(Attachment::getDelFlag, 0));
            if (CollUtil.isNotEmpty(attachments)) {
                List<Long> ids = attachments.stream().map(Attachment::getId).collect(Collectors.toList());
                super.removeBatchByIds(ids);
            }
            //保存多个附件
            List<Attachment> list = new ArrayList<>();
            attachmentList.forEach(x -> {
                Attachment attachment = new Attachment();
                attachment.setBusinessType(businessType.getType());
                attachment.setBusinessId(businessId);
                attachment.setFileUrl(x.getFileUrl());
                attachment.setFileName(x.getFileName());
                list.add(attachment);
            });
            super.saveOrUpdateBatch(list);
        }
    }

    @Override
    public List<AttachmentVO> listAttachment(AttachmentTypeEnum businessType, Long businessId) {
        List<Attachment> attachments = super.list(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getBusinessType, businessType.getType())
                .eq(Attachment::getBusinessId, businessId)
                .eq(Attachment::getDelFlag, 0));

        return BeanCopierUtil.copyList(attachments, AttachmentVO.class);
    }

    @Override
    public AttachmentVO getAttachmentById(Long attachmentId) {
        Attachment attachment = super.getOne(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getId, attachmentId));
        return BeanCopierUtil.copyBean(attachment, AttachmentVO.class);
    }

    @Override
    public Long addAttachment(AttachmentRequestVO requestVO, AttachmentTypeEnum businessType, Long businessId) {
        Attachment attachment = new Attachment();
        attachment.setBusinessType(businessType.getType());
        attachment.setBusinessId(businessId);
        attachment.setFileUrl(requestVO.getFileUrl());
        attachment.setFileName(requestVO.getFileName());

        super.save(attachment);
        return attachment.getId();
    }

    @Override
    public void deleteByBusinessId(AttachmentTypeEnum businessType, Long businessId) {
        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getDelFlag, 2)
                .eq(Attachment::getBusinessId, businessId)
                .eq(Attachment::getBusinessType, businessType.getType()));
    }

    @Override
    public Long saveAttachment(AttachmentRequestVO requestVO) {
        Attachment attachment = new Attachment();
        attachment.setFileUrl(requestVO.getFileUrl());
        attachment.setFileName(requestVO.getFileName());

        super.save(attachment);

        return attachment.getId();
    }

    @Override
    public void updateBusiness(Long id, Long businessId,String fileUrl,String fileName) {
        if (NumberUtil.isNullOrZero(id)  || NumberUtil.isNullOrZero(businessId)||fileUrl==null||fileName==null) {
            throw new ParamValidateException("保存附件时关键信息为空");
        }
        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getBusinessId, businessId)
                .set(Attachment::getFileUrl, fileUrl)
                .set(Attachment::getFileName, fileName)
                .eq(Attachment::getId, id));
    }

    //更新文件名和文件URL
    @Override
    public void updateFileNameANDFileUrl(Long id, String fileUrl, String fileName) {
        if (NumberUtil.isNullOrZero(id)  ||fileUrl==null||fileName==null) {
            throw new ParamValidateException("保存附件时关键信息为空");
        }
        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getFileUrl, fileUrl)
                .set(Attachment::getFileName, fileName)
                .eq(Attachment::getId, id));
    }

    @Override
    public void updateBusiness(Long id, AttachmentTypeEnum businessType, Long businessId) {
        if (NumberUtil.isNullOrZero(id) || businessType == null || NumberUtil.isNullOrZero(businessId)) {
            throw new ParamValidateException("保存附件时关键信息为空");
        }

        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getBusinessType, businessType.getType())
                .set(Attachment::getBusinessId, businessId)
                .eq(Attachment::getId, id));
    }

    @Override
    public DownloadAgreementVO getAttachmentInputStream(long attachmentId, String agreementName) {
        Attachment attachment = super.getById(attachmentId);
        ValidateUtils.isNullException(attachment, "该合同附件不存在，请联系管理员");
        if (StringUtils.isBlank(attachment.getFileUrl())) {
            throw new ParamValidateException("附件 url 地址不存在，请联系管理员");
        }

        InputStream inputStream = sysFileService.getFileByFileUrl(attachment.getFileUrl());

        DownloadAgreementVO agreementVO = new DownloadAgreementVO();
        agreementVO.setFileStream(inputStream);
        agreementVO.setFileName(agreementName + FileNameUtil.getPrefix(attachment.getFileName()));
        return agreementVO;
    }
}
