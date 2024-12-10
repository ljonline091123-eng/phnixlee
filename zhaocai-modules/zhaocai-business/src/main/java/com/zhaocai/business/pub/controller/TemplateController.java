package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.utils.BookmarkUtils;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.TemplateSaveRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.business.pub.vo.res.TemplateVO;
import com.zhaocai.business.sdk.bean.EditParams;
import com.zhaocai.business.sdk.bean.PreviewParams;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
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

    @Autowired
    private YOZOfileUtils yozOfileUtils;


    @Autowired
    private IAttachmentService attachmentService;

    /**
     * 据模板id查询附件，并利用yozo文档中台预览附件,返回预览文件的url
     */
    @GetMapping("/PreviewFile")
    @ApiOperation(value = "预览附件文件")
    public ResultData<String> PreviewFile(@RequestParam Long id) {
        TemplateVO template = templateService.detail(id);
        String fileName = template.getFileName();
        String fileUrl = template.getFileUrl();
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

    //新增和修改范本时，word文档返回文档中台的文件编辑URL，图片和pdf格式是显示预览文件
    @GetMapping("/getEditFileURL")
    @ApiModelProperty(value = "文档中台的文件编辑URL")
    public ResultData<String> getEditFileURL(AttachmentVO requestVO) {
        Long attachmentId = requestVO.getId();
        String fileName = requestVO.getFileName();
        String fileUrl = requestVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法编辑文件！！！");
        }
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        //word文件则编辑，其他文件则预览
        if (yozOfileUtils.isWordExtension(suffix)) {
            return ResultData.data(attachmentService.editWordURL(attachmentId,fileName,fileUrl));
        } else if (yozOfileUtils.isImageExtension(suffix)){
            return ResultData.data(attachmentService.viewImageURL(fileName,fileUrl));
        }else if (yozOfileUtils.isPdfExtension(suffix)){
            return ResultData.data(attachmentService.viewPDFFileURL(fileName,fileUrl));
        }else {
            return ResultData.fail("上传文件类型错误，不支持该类型文件");
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
