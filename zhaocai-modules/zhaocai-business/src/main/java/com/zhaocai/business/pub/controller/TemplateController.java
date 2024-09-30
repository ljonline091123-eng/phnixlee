package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.TemplateSaveRequestVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.business.pub.vo.res.TemplateVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
