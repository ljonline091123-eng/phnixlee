package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.vo.res.ReceiptThirdRequestVO;
import com.zhaocai.business.receipt.vo.res.ReceiptThirdResponseVO;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.business.receipt.service.IPushReceiptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 推送数据Controller
 *
 * @author cff
 * @date 2024-09-11
 */
@Api(value = "第三方-推送数据接口")
@Slf4j
@RestController
@RequestMapping("/pushReceipt")
public class PushReceiptController extends BladeController {

    @Autowired
    private IPushReceiptService pushReceiptService;

    /**
     * 推送采购订单状态
     */
    @PostMapping(value = "/modifyPurchaseState")
    @ApiOperation(value = "推送采购订单状态")
    public ResultData<Boolean> modifyPurchaseState(@Validated @RequestBody ReceiptThirdRequestVO deviceLedgerVO) {
        return ResultData.status(pushReceiptService.modifyPurchaseState(deviceLedgerVO));
    }

    /**
     * 推送结算单状态
     */
    @PostMapping(value = "/modifyContractsettlementState")
    @ApiOperation(value = "推送结算单状态")
    public ResultData<Boolean> modifyContractsettlementState(@Validated @RequestBody ReceiptThirdRequestVO deviceLedgerVO) {
        return ResultData.status(pushReceiptService.modifyContractsettlementState(deviceLedgerVO));
    }

    /**
     * 推送设备租赁台账状态
     */
    @PostMapping(value = "/modifyDeviceLedgerState")
    @ApiOperation(value = "推送设备租赁台账状态")
    public ResultData<Boolean> modifyDeviceLedgerState(@Validated @RequestBody ReceiptThirdRequestVO deviceLedgerVO) {
        return ResultData.status(pushReceiptService.modifyDeviceLedgerState(deviceLedgerVO));
    }

    /**
     * 推送租赁周材台账账状态
     */
    @PostMapping(value = "/modifyTurnLedgerState")
    @ApiOperation(value = "推送租赁周材台账账状态")
    public ResultData<Boolean> modifyTurnLedgerState(@Validated @RequestBody ReceiptThirdRequestVO deviceLedgerVO) {
        return ResultData.status(pushReceiptService.modifyTurnLedgerState(deviceLedgerVO));
    }

    /**
     * 推送材料对账单供应商状态
     */
    @PostMapping(value = "/reconciliationState")
    @ApiOperation(value = "推送材料对账单供应商状态")
    public ResultData<Boolean> reconciliationState(@Validated @RequestBody ReceiptThirdRequestVO deviceLedgerVO) {
        return ResultData.status(pushReceiptService.reconciliationState(deviceLedgerVO));
    }
}
