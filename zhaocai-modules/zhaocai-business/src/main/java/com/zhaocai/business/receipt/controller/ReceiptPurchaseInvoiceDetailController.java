package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptPurchaseInvoiceDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  发货单详情 Controller
 *
 * @author cff
 * @date 2024-09-13
 */
@RestController
@RequestMapping("/invoiceDetail")
public class ReceiptPurchaseInvoiceDetailController extends BladeController {
    @Autowired
    private IReceiptPurchaseInvoiceDetailService receiptPurchaseInvoiceDetailService;


}
