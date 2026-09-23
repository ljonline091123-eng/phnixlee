package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptReconciliationService;
import com.zhaocai.business.receipt.vo.req.ReceiptReconciliationVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 材料对账单Controller
 * 
 * @author zhangxu
 * @date 2024-09-06
 */
@Api(value = "第三方-材料对账单接口")
@RestController
@RequestMapping("/underling/reconciliation")
public class UnderlingReceiptReconciliationController extends BladeController {
    @Autowired
    private IReceiptReconciliationService receiptReconciliationService;

    /**
    * 接收材料对账单
    */
    @PostMapping("/receiveReconciliation")
    public ResultData<Boolean> receiveReconciliation(@RequestBody ReceiptReconciliationVO reconciliationVO){
        //添加材料对账单和对账单详情列表
        return ResultData.status(receiptReconciliationService.saveReconciliation(reconciliationVO));
    }
}
