package com.zhaocai.business.agreement.controller;

import com.zhaocai.business.agreement.service.IMarketMaterialContractService;
import com.zhaocai.business.agreement.vo.req.MarketMaterialContractQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementCreateBaseInfoVO;
import com.zhaocai.business.agreement.vo.res.MarketMaterialContractListVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 易料采购合同信息Controller
 *
 * @author lsn
 * @date 2024-10-22
 */
@RestController
@RequestMapping("/marketMaterialContract")
@Api(value = "合同管理")
public class MarketMaterialContractController extends BladeController {

    @Autowired
    private IMarketMaterialContractService marketMaterialContractService;

    /**
     * 获取可签订的易料采购合同
     */
    @GetMapping("/listMarketMaterialContract")
    @ApiOperation(value = "获取可签订的易料采购合同")
    public ResultData<PageResult<MarketMaterialContractListVO>> listMarketMaterialContract(@Valid MarketMaterialContractQueryVO queryVO) {
        return ResultData.data(marketMaterialContractService.listMarketMaterialContract(queryVO));
    }

    /**
     * 获取创建合同的基本信息
     */
    @PostMapping("/getAgreementCreateInfo")
    @ApiOperation(value = "获取创建合同的基本信息")
    public ResultData<AgreementCreateBaseInfoVO> getAgreementCreateInfo(@Valid @RequestBody MarketMaterialContractQueryVO queryVO) {
        return ResultData.data(marketMaterialContractService.getAgreementCreateInfo(queryVO));
    }

    /**
     * 获取创建合同的基本信息
     */
    @PostMapping("/agreementCreateAttachmentHandle")
    @ApiOperation(value = "获取创建合同的基本信息")
    public long agreementCreateAttachmentHandle(@Valid @RequestBody AttachmentVO attachmentVO) {
        return marketMaterialContractService.agreementCreateAttachmentHandle(attachmentVO);
    }


}
