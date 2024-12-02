package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.pub.utils.Sender;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.TemplateSaveRequestVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.business.pub.vo.res.TemplateVO;
import com.zhaocai.business.sdk.bean.EditParams;
import com.zhaocai.business.sdk.bean.PreviewParams;
import com.zhaocai.business.sdk.bean.WaterMark;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板管理Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@Api(value = "模板管理")
@RestController
@RequestMapping("/template")
public class TemplateController extends BladeController {

    @Autowired
    private ITemplateService templateService;

    /**
     * 据模板id查询附件，并利用yozo文档中台预览附件,返回预览文件的url
     */
    @GetMapping("/PreviewFile")
    @ApiOperation(value = "预览附件文件")
    public ResultData<String> PreviewFile(@RequestParam Long id) {
        ResultData<String> result = null;
        TemplateVO template = templateService.detail(id);
        String fileName = template.getFileName();
        String fileUrl = template.getFileUrl();
        String HtmlName = YOZOfileUtils.removeSuffix(fileName);
        String suffix = YOZOfileUtils.getSuffix(fileName).toLowerCase();
        Path path = YOZOfileUtils.downloadFile(fileUrl, YOZOfileUtils.createTempFilePath(fileName));
        String response;
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
           if (YOZOfileUtils.isWordExtension(suffix)) {
               // 是否显示修订
               params.setAcceptTracks(false);
               // 允许复制
               params.setCopy(false);
               // 只允许打开一次
               params.setPreviewNumber(5);
               response= Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
                System.out.println("预览Office文件响应结果：");
                System.out.println(response);
           } else if(YOZOfileUtils.isPdfExtension(suffix)){
               // 允许复制
               params.setCopy(true);
               response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
               System.out.println("预览pdf文件响应结果：");
               System.out.println(response);
               String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
               System.out.println(viewUrl);
           } else if (YOZOfileUtils.isImageExtension(suffix)) {
               response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
               System.out.println("预览图片文件响应结果：");
               System.out.println(response);
               String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
               System.out.println(viewUrl);
           }else {
               return ResultData.fail("无法预览该文件格式！");
           }
           String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
           System.out.println(viewUrl);
           result = ResultData.data(viewUrl);
        } catch (Exception e) {
            throw new RuntimeException("生成文件预览url失败", e);
        }
        if (result == null) {
            result = ResultData.fail("生成文件预览url失败！");
        }
        //删除生成的临时文件
        System.out.println("删除文件路径:" + path.toString());
        YOZOfileUtils.deleteTempFilePath(path.toString());
        return result;
    }

    //新增和修改范本时，word文档返回文档中台的文件编辑URL，图片和pdf格式是显示预览文件
    @GetMapping("/getEditFileURL")
    @ApiModelProperty(value = "文档中台的文件编辑URL")
    public ResultData<String> getEditFileURL(AttachmentRequestVO requestVO) {
        String fileName = requestVO.getFileName();
        String fileUrl = requestVO.getFileUrl();
        String HtmlName = YOZOfileUtils.removeSuffix(fileName);
        String suffix = YOZOfileUtils.getSuffix(fileName).toLowerCase();
        Path path = YOZOfileUtils.downloadFile(fileUrl, YOZOfileUtils.createTempFilePath(fileName));
        //当前登录用户信息
        Long loginUserId = SecurityUtils.getUserId();
        String loginUserName = SecurityUtils.getUsername();
        try {
            if (YOZOfileUtils.isWordExtension(suffix)) {
                // 编辑文档的-组织请求参数
                EditParams params = new EditParams();
                params.setFilePath(path.toString());
                params.setFileName(fileName);
                params.setUserInfo(loginUserId.toString(), loginUserName);
                params.setUserRight(EditParams.USERRIGHT_EDIT);
                // 自动保存
                params.setSaveFlag(true);
                // 回调地址支持2中方式获取文件，请根据需要按照接口规范实现接口
                params.setCallbackUrl("192.168.30.42:8052/business/template/fileUpload?version=cs&type=fb");
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
                params.setBookMarkListRange("甲方,乙方,金额,签订日期");

                String response = Sender.post(EditParams.URL_EDIT, EditParams.CONVERT_TYPE_EDIT, params.getRequestBody());
                System.out.println("编辑响应结果：");
                System.out.println(response);
                String editUrl = new JSONObject(response).optJSONObject("data").optString("editUrl");
                System.out.println(editUrl);
                return ResultData.data(editUrl);
            } else if (YOZOfileUtils.isImageExtension(suffix)){
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
                String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PIC, params.getRequestBody());
                System.out.println("预览图片文件响应结果：");
                System.out.println(response);
                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                System.out.println(viewUrl);
                return ResultData.data(viewUrl);
            }else if (YOZOfileUtils.isPdfExtension(suffix)){
                // 组织请求参数
                PreviewParams params = new PreviewParams();
                // 设置要预览的文件
                params.setFilePath(path.toString());
                params.setFileName(fileName);
                params.setHtmlName(HtmlName);
                params.setHtmlTitle(HtmlName);
                // 是否可打印
                params.setPrintMenu(true, false);
                // 允许复制
                params.setCopy(true);
                // 设置可下载
                params.setDownloadMenu(true, "测试1.pdf");
                String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_PDF, params.getRequestBody());
                System.out.println("预览pdf文件响应结果：");
                System.out.println(response);
                String viewUrl = new JSONObject(response).optJSONObject("data").optString("viewUrl");
                System.out.println(viewUrl);
                return ResultData.data(viewUrl);
            }else {
                return ResultData.fail("上传文件类型错误，不支持该类型文件");
            }
        } catch (Exception e) {
            throw new RuntimeException("生成文件编辑url失败", e);
        }


    }



    /**
     * 列表查询
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "列表查询")
    public ResultData<PageResult<TemplateListVO>> listPage(TemplateListQueryVO queryVO) {
            return ResultData.data(templateService.listPage(queryVO));
    }

    /**
     * 合同类型列表
     */
    @GetMapping("/contractTypeList")
    @ApiOperation(value = "合同类型列表")
    public ResultData<List<Map<String, Object>>> contractTypeList() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (ProcurementPlanTypeEnum value : ProcurementPlanTypeEnum.values()) {
            Map<String, Object> map = new HashMap<>(50);
            map.put("value",value.getType()+"");
            map.put("label",value.getDesc());
            resultList.add(map);
        }
        return ResultData.data(resultList);
    }

    /**
     * 列表查询
     */
    @GetMapping("/fanListPage")
    @ApiOperation(value = "列表查询")
    public ResultData<PageResult<TemplateListVO>> fanListPage(TemplateListQueryVO queryVO) {
        return ResultData.data(templateService.fanListPage(queryVO));
    }

    /**
     * 采购方案选择招标文件模板切换
     */
    @GetMapping("/switchListPage")
    @ApiOperation(value = "列表查询")
    public ResultData<PageResult<TemplateListVO>> switchListPage(TemplateListQueryVO queryVO) {
        return ResultData.data(templateService.switchListPage(queryVO));
    }


    /**
     * 保存模板
     */
    @PostMapping("/saveTemplate")
    @ApiOperation(value = "保存模板")
    public ResultData<Boolean> saveTemplate(@RequestBody TemplateSaveRequestVO requestVO) {
        templateService.saveTemplate(requestVO);
        return ResultData.success();
    }

    /**
     * 删除模板
     */
    @PostMapping("/deleteTemplate")
    @ApiOperation(value = "删除模板")
    public ResultData<Boolean> deleteTemplate(@RequestParam Long id) {
        templateService.deleteTemplate(id);
        return ResultData.success();
    }

    /**
     * 模板详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "模板详情")
    public ResultData<TemplateVO> detail(@RequestParam Long id) {
        return ResultData.data(templateService.detail(id));
    }
}
