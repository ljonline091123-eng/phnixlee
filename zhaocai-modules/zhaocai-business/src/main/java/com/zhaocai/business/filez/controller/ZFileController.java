package com.zhaocai.business.filez.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.filez.service.IFileZCallBackService;
import com.zhaocai.business.filez.service.IFileZTaskService;
import com.zhaocai.business.filez.util.DateUtil;
import com.zhaocai.business.filez.util.MimeTypeUtil;
import com.zhaocai.business.filez.vo.req.FileZCallBackVO;
import com.zhaocai.business.filez.vo.res.*;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.common.core.context.SecurityContextHolder;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.model.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@Slf4j
@Api(value = "zfile回调API")
@RestController
@RequestMapping("/zfile")
public class ZFileController extends BladeController {

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private IFileZCallBackService fileZService;

    @Autowired
    private IFileZTaskService fileZTaskService;

    @GetMapping("/{docId}/content")
    @ApiOperation(value = "获取文件内容")
    public void getContent(@PathVariable("docId") String docId, HttpServletResponse response) {
        Attachment attachment = attachmentService.getById(docId);
        ByteArrayInputStream byteArrayInputStream = null;
        try (InputStream is = sysFileService.getFileByFileUrl(attachment.getFileUrl())) {
            if (is != null) {
                String docName = attachment.getFileName();
                String mime = MimeTypeUtil.MIME_TYPE_MAP.getContentType(docName);
                response.setContentType(mime);
                response.setHeader("Content-disposition", "filename=" + java.net.URLEncoder.encode(docName, "UTF-8"));
                response.setStatus(HttpServletResponse.SC_OK);
                try (BufferedInputStream bis = new BufferedInputStream(is);
                     ServletOutputStream out = response.getOutputStream()) {
                    int numRead = -1;
                    byte[] data = new byte[8192];
                    while ((numRead = bis.read(data)) > 0) {
                        out.write(data, 0, numRead);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                response.sendError(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(byteArrayInputStream);
        }

    }

    /**
     * 存储文件内容
     */
    @PostMapping("/{docId}/content")
    @ApiOperation(value = "存储文件内容")
    public void saveContent(@PathVariable("docId") String docId, MultipartFile file,
                            HttpServletRequest request, HttpServletResponse response) throws IOException {
        Attachment attachment = attachmentService.getById(docId);
        if (attachment == null) {
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        String url = sysFileService.uploadFile(file);
        
        log.info("文件上传成功，文件路径：" + url);
        if (url != null) {
            attachment.setFileUrl(url);
            attachmentService.update(new LambdaUpdateWrapper<Attachment>()
                    .set(Attachment::getFileUrl,url)
                    .eq(Attachment::getId,docId));
            if (AttachmentTypeEnum.AGREEMENT_ORIGINAL.equalsType(attachment.getBusinessType())) {
                // 合同需要更新任务
                fileZTaskService.updateTaskStatusByAttachment(attachment.getId(), FileZTaskBusinessEnum.AGREEMENT_CREATE);
            }
        }

        DocMeta docMeta = getDocMeta(attachment);
       log.info("文件元数据 docMeta:{}",JSON.toJSONString(docMeta));
        JSONObject obj = (JSONObject) JSON.toJSON(docMeta);
        if (obj != null) {
            response.setContentType("text/x-json");
            response.setCharacterEncoding("UTF-8");
            JSONObject.writeJSONString(response.getWriter(), docMeta);
        } else {
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    @GetMapping("/{docId}/meta")
    public void getMeta(@PathVariable("docId") String docId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Attachment attachment = attachmentService.getById(docId);
        if (attachment == null) {
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        DocMeta docMeta = getDocMeta(attachment);
        response.setContentType("text/x-json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSONObject.toJSONString(docMeta));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profiles")
    @ApiOperation(value = "获取当前用户信息")
    public void profile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = getUser();
        response.setContentType("text/x-json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSONObject.toJSONString(user));
    }

    /**
     * 联想文档回调
     *
     * @param callBack
     */
    @PostMapping("/callback/contentUpdate")
    public void contentUpdateCallback(@RequestBody FileZCallBackVO callBack,HttpServletRequest request) {
        log.info("[联想文档内容回调] - 回调数据:{}",JSON.toJSONString(callBack));
        String fileZTaskIdValue = getCookieValue(request,"fileZTaskId");
        if (StringUtils.isBlank(fileZTaskIdValue)) {
            log.error("[联想文档内容回调] - 获取 fileZTaskId 为空");
        } else {
            long fileZTaskId = Long.parseLong(fileZTaskIdValue);
            fileZService.contentUpdateHandler(callBack,fileZTaskId);
        }
    }



    private User getUser() {
        User user = new User();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        //如果没有登录，先返回家的用户
        if (loginUser == null) {
            user.setId("1");
            user.setEmail("default@qq.com");
            user.setName("default");
            user.setDisplayName("default");
            user.setPhotoUrl("123456");
            return user;
        }
        SysUser sysUser = loginUser.getSysUser();
        user.setId(sysUser.getUserId().toString());
        user.setEmail(sysUser.getEmail());
        user.setName(SecurityContextHolder.getUserName());
        user.setDisplayName(SecurityContextHolder.getUserName());
        user.setPhotoUrl(sysUser.getPhonenumber());
        return user;
    }

    private DocMeta getDocMeta(Attachment attachment) {
        String newFileId = attachment.getId().toString();
        DocMeta docMeta = DocMeta.builder().id(newFileId).size(Long.valueOf(0)).name(attachment.getFileName())
                .createdBy(getUser()).build();
        docMeta.setVersion(DateUtil.getCurrentTimestamp());
        docMeta.setCreatedAt(new Date());
        docMeta.setModifiedAt(new Date());
        DocPermission docPermission = new DocPermission();
        docPermission.setWrite(true);
        docPermission.setRead(true);
        docMeta.setPermissions(docPermission);
        Extension extension = new Extension();
        docMeta.setExtension(extension);
        if (docMeta.isNeedWaterMark()) {
            WaterMark waterMark = new WaterMark();
            docMeta.setWaterMark(waterMark);
        }
        docMeta.setDescription(attachment.getFileName());
        return docMeta;
    }
}
