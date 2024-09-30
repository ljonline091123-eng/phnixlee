package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptPurchaseInvoiceService;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseInvoiceQueryVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 *  发货单 Controller
 *
 * @author cff
 * @date 2024-09-13
 */
@RestController
@RequestMapping("/invoice")
public class ReceiptPurchaseInvoiceController extends BladeController {
    @Autowired
    private IReceiptPurchaseInvoiceService receiptPurchaseInvoiceService;
    /**
     * 发货单列表
     */
    @GetMapping("/invoiceListPage")
    @ApiOperation(value = "发货单列表")
    public ResultData<PageResult<ReceiptPurchaseInvoiceVO>> invoiceListPage(ReceiptPurchaseInvoiceQueryVO purchaseInvoiceQueryVO)
    {
        return ResultData.data(receiptPurchaseInvoiceService.invoiceListPage(purchaseInvoiceQueryVO));
    }


    /**
     * 二维码发货单 详细信息
     */
    @GetMapping(value = "/getQrCodeInfo/{id}", produces = "application/json;charset=UTF-8")
    @ApiOperation(value = "根据id查询发货单")
    public ResultData<ReceiptPurchaseInvoiceVO> getQrCodeInfo(@PathVariable("id") Long id)
    {
        return ResultData.data(receiptPurchaseInvoiceService.getQrCodeInfo(id));
    }

    /**
     * 发货单 详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation(value = "根据id查询发货单")
    public ResultData<ReceiptPurchaseInvoiceVO> getPushPurchaseInfo(@PathVariable("id") Long id)
    {
        return ResultData.data(receiptPurchaseInvoiceService.detail(id));
    }


    /**
     * 保存发货单
     */
    @PostMapping("/saveInvoice")
    @Log(title = "保存发货单", businessType = BusinessType.INSERT)
    public ResultData saveInvoice(@RequestBody ReceiptPurchaseInvoiceVO purchaseInvoiceVO){
        return ResultData.data(receiptPurchaseInvoiceService.saveInvoice(purchaseInvoiceVO));
    }

}
