package com.zhaocai.business.receipt.controller;

import cn.hutool.core.io.resource.InputStreamResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseQueryVO;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.business.receipt.service.IReceiptPurchaseService;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import com.zhaocai.common.core.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 材料对账单Controller
 *
 * @author cff
 * @date 2024-09-06
 */
@Api(value = "第三方-采购订单接口")
@Slf4j
@RestController
@RequestMapping("/purchase")
public class ReceiptPurchaseController extends BladeController {


    @Autowired
    private IReceiptPurchaseService receiptPurchaseService;

    /**
     * 采购订单列表
     */
    @GetMapping("/purchaseListPage")
    @ApiOperation(value = "采购订单列表")
    public ResultData<PageResult<ReceiptPurchaseVO>> purchaseListPage(ReceiptPurchaseQueryVO receiptPurchaseVO)
    {
        return ResultData.data(receiptPurchaseService.purchaseListPage(receiptPurchaseVO));
    }

    /**
     * 推送采购订单 详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation(value = "根据id查询采购订单")
    public ResultData<ReceiptPurchaseVO> getPushPurchaseInfo(@PathVariable("id") Long id)
    {
        return ResultData.data(receiptPurchaseService.detail(id));
    }


    /**
     * 推送采购订单 详细信息
     */
    @GetMapping(value = "/invoice/{id}")
    @ApiOperation(value = "根据id查询采购订单")
    public ResultData<ReceiptPurchaseVO> getInvoiceInfo(@PathVariable("id") Long id)
    {
        return ResultData.data(receiptPurchaseService.getInvoiceInfo(id));
    }


    /**
     * 推送二维码
     */
    @GetMapping(value = "/invoiceQrCode/{id}", produces = "application/json;charset=UTF-8")
    @ApiOperation(value = "推送二维码")
    public ResultData invoiceQrCode(@PathVariable("id") Long id, HttpServletResponse response) throws WriterException, IOException {
         return  ResultData.data(receiptPurchaseService.generateQrCodeImage(id,response));
    }
}
