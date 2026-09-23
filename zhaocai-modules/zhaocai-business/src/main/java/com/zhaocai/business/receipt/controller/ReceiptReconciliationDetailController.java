package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.impl.ReceiptReconciliationDetailServiceImpl;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 材料对账单详情Controller
 *
 * @author zhagnxu
 * @date 2024-09-07
 */
@RestController
@Api(value = "材料对账单详情", tags = "材料对账单详情")
public class ReceiptReconciliationDetailController extends BladeController {
    @Autowired
    private ReceiptReconciliationDetailServiceImpl reconciliationDetailService;
}
