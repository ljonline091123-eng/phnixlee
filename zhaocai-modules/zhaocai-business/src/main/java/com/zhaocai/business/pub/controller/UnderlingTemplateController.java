package com.zhaocai.business.pub.controller;

import com.zhaocai.business.agreement.vo.req.AgreementJkpthtListQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementJkpthtListVO;
import com.zhaocai.business.agreement.vo.res.AgreementUnderlingDetailVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.UnderlingTemplateListQueryVO;
import com.zhaocai.business.pub.vo.res.UnderlingTemplateDetailVO;
import com.zhaocai.business.pub.vo.res.UnderlingTemplateListVO;
import com.zhaocai.common.core.bean.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



/**
 * 引用招采补充协议接口  模板
 */
@Api(value = "第三方-模板接口")
@RestController
@RequestMapping("/underling/template")
public class UnderlingTemplateController extends BladeController {

    @Autowired
    private ITemplateService templateService;



    /**
     * 列表查询
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "列表查询")
    public PageResult<UnderlingTemplateListVO> listPage(UnderlingTemplateListQueryVO queryVO) {
        return templateService.UnderlinglistPage(queryVO);
    }

    /**
     * 合同详情
     */
    @GetMapping("/detail")
    public UnderlingTemplateDetailVO detail(@RequestParam Long id) {
        return templateService.UnderlingDetail(id);
    }
}

