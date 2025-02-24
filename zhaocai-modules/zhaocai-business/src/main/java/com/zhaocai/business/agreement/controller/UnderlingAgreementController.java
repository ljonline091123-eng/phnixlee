package com.zhaocai.business.agreement.controller;

import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.agreement.vo.req.AgreementJkpthtListQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementJkpthtListVO;
import com.zhaocai.business.agreement.vo.res.AgreementUnderlingDetailVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支出合同合同登记引用招采合同接口 支出合同引用招采引用 合同登记
 */
@Api(value = "第三方-合同接口")
@RestController
@RequestMapping("/underling/agreement")
public class UnderlingAgreementController extends BladeController {

    @Autowired
    private IAgreementService agreementService;

    /**
     * 获取合同列表
     * @param queryVO
     * @return
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "获取合同列表")
    public PageResult<AgreementJkpthtListVO> listPage(AgreementJkpthtListQueryVO queryVO) {
        return agreementService.listAgreementJkpthtPage(queryVO);
    }

    /**
     * 合同详情
     */
    @GetMapping("/detail")
    public AgreementUnderlingDetailVO detail(@RequestParam Long id) {
        return agreementService.getAgreementUnderlingDetail(id);
    }
}
