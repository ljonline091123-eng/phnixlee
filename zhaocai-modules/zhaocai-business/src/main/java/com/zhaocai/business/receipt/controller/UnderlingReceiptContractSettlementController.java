package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptContractSettlementService;
import com.zhaocai.business.receipt.service.IReceiptPurchaseService;
import com.zhaocai.business.receipt.service.IReceiptReconciliationService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettlementVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.business.receipt.vo.req.ReceiptReconciliationVO;
import com.zhaocai.common.core.utils.JacksonUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.DataInput;

/**
 * 结算单Controller
 *
 * @author cff
 * @date 2024-09-06
 */
@Api(value = "第三方-结算单接口")
@Slf4j
@RestController
@RequestMapping("/underling/contractsettlement")
public class UnderlingReceiptContractSettlementController extends BladeController {


    @Autowired
    private IReceiptContractSettlementService  receiptContractSettlementService;

    /**
    * 接收结算单
    */
    @PostMapping("/receiveContractSettlement")
    @Log(title = "结算单管理", businessType = BusinessType.INSERT)
    public ResultData receiveContractSettlement(@RequestBody ReceiptContractSettlementVO pushContractSettlementVO){
        // 将RequestBody转换为JSON对象
        log.info("接收结算单管理数据：" + JacksonUtil.toJsonString(pushContractSettlementVO));
        // 打印JSON对象
        System.out.println("接收结算单管理数据：" + JacksonUtil.toJsonString(pushContractSettlementVO));
        return ResultData.data(receiptContractSettlementService.insertPushContractSettlement(pushContractSettlementVO));
    }
}
