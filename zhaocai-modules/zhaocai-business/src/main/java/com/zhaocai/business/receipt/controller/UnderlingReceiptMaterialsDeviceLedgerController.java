package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerService;
import com.zhaocai.business.receipt.service.IReceiptPurchaseService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.common.core.utils.JacksonUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备租赁台账Controller
 *
 * @author cff
 * @date 2024-09-06
 */
@Api(value = "第三方-设备租赁台账接口")
@Slf4j
@RestController
@RequestMapping("/underling/materialsDeviceLedger")
public class UnderlingReceiptMaterialsDeviceLedgerController extends BladeController {


    @Autowired
    private IReceiptMaterialsDeviceLedgerService ledgerService;

    /**
     * 接收设备租赁台账
     */
    @PostMapping("/receiptMaterialsDeviceLedger")
    @Log(title = "接收设备租赁台账", businessType = BusinessType.INSERT)
    public ResultData receiptMaterialsDeviceLedger(@RequestBody ReceiptMaterialsDeviceLedgerVO pushPurchaseVO){
        // 将RequestBody转换为JSON对象
        log.info("接收设备租赁台账数据：" + JacksonUtil.toJsonString(pushPurchaseVO));
        // 打印JSON对象
        System.out.println("接收设备租赁台账数据：" +JacksonUtil.toJsonString(pushPurchaseVO));
        return ResultData.data(ledgerService.insertMaterialsDeviceLedger(pushPurchaseVO));
    }
}
