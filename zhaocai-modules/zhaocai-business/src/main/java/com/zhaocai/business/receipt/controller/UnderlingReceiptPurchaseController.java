package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptContractSettlementService;
import com.zhaocai.business.receipt.service.IReceiptPurchaseService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettlementVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.common.core.utils.JacksonUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 采购订单Controller
 *
 * @author cff
 * @date 2024-09-06
 */
@Api(value = "第三方-采购订单接口")
@Slf4j
@RestController
@RequestMapping("/underling/purchase")
public class UnderlingReceiptPurchaseController extends BladeController {


    @Autowired
    private IReceiptPurchaseService receiptPurchaseService;

    /**
     * 接收采购订单
     */
    @PostMapping("/receiptPurchase")
    @Log(title = "接收采购订单", businessType = BusinessType.INSERT)
    public ResultData receiptPurchase(@RequestBody ReceiptPurchaseVO pushPurchaseVO) {
        // 将RequestBody转换为JSON对象
        log.info("接收采购订单数据：" + JacksonUtil.toJsonString(pushPurchaseVO));
        // 打印JSON对象
        System.out.println("接收采购订单数据：" +JacksonUtil.toJsonString(pushPurchaseVO));
        return ResultData.data(receiptPurchaseService.insertPushPurchase(pushPurchaseVO));
    }


    /**
     * 推送二维码
     */
    @GetMapping(value = "/invoiceDetail/{id}", produces = "application/json;charset=UTF-8")
    @ApiOperation(value = "推送二维码")
    public ResultData invoiceDetail(@PathVariable("id") Long id, HttpServletResponse response) throws WriterException, IOException {
        return  ResultData.data(receiptPurchaseService.generateQrCodeImage(id,response));
    }
}
