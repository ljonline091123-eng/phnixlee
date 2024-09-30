package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptContractSettlementService;
import com.zhaocai.business.receipt.vo.res.query.ReceiptContractSettlementQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettlementVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 材料对账单Controller
 *
 * @author zhangxu
 * @date 2024-09-06
 */
@Api(value = "第三方-结算单接口")
@Slf4j
@RestController
@RequestMapping("/contractsettlement")
public class ReceiptContractSettlementController extends BladeController {


    @Autowired
    private IReceiptContractSettlementService  receiptContractSettlementService;


    /**
     * 结算单列表
     */
    @GetMapping("/contractsettlementListPage")
    @ApiOperation(value = "结算单列表")
    public ResultData<PageResult<ReceiptContractSettlementVO>> contractsettlementListPage(ReceiptContractSettlementQueryVO receiptContractSettlementQueryVO)
    {
        return ResultData.data(receiptContractSettlementService.contractsettlementListPage(receiptContractSettlementQueryVO));
    }

    /**
     * 结算单 详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation(value = "根据id查询结算单")
    public ResultData<ReceiptContractSettlementVO> getPushPurchaseInfo(@PathVariable("id") Long id)
    {
        return ResultData.data(receiptContractSettlementService.detail(id));
    }
}
