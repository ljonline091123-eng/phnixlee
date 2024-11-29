package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.demo.app.Sender;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.TemplateSaveRequestVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.business.pub.vo.res.TemplateVO;
import com.zhaocai.business.sdk.bean.PreviewParams;
import com.zhaocai.business.sdk.bean.WaterMark;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
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
        Path path = YOZOfileUtils.downloadFile(fileUrl, YOZOfileUtils.createTempFilePath(fileName));
        // 组织请求参数
        PreviewParams params = new PreviewParams();
        try {
            // 设置要预览的文件
            params.setFilePath(path.toString());
            params.setFileName(fileName);
            params.setHtmlName(HtmlName);
            params.setHtmlTitle(HtmlName);
            // 允许复制
            params.setCopy(false);
            // 签批
            params.setSignature(true, "http://www.abc.com/xxx");
            // 是否可打印
            params.setPrintMenu(true, false);
            // 是否显示修订
            params.setAcceptTracks(false);
            // 设置可下载
            params.setDownloadMenu(true, "测试1.docx");
            // 只允许打开一次
            params.setPreviewNumber(5);

            String response = Sender.post(PreviewParams.URL_PREVIEW, PreviewParams.CONVERT_TYPE_PREVIEW_OFFICE, params.getRequestBody());
            System.out.println("预览Office文件响应结果：");
            System.out.println(response);
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
        System.out.println("删除文件路径" + path.toString());
        YOZOfileUtils.deleteTempFilePath(path.toString());
        return result;
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
