package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.impl.ReceiptReconciliationServiceImpl;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailPageVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationListVO;
import com.zhaocai.business.receipt.vo.res.query.ReconciliationQueryVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 材料对账单Controller
 *
 * @author zhangxu
 * @date 2024-09-06
 */
@RestController
@RequestMapping("/reconciliation")
@Api(value = "材料对账单对象", tags = "材料对账单对象")
public class ReceiptReconciliationController extends BladeController {
    @Autowired
    private ReceiptReconciliationServiceImpl reconciliationService;

    /**
     * 分页查询材料对账单列表
     */
    @PostMapping("/page")
    @ApiOperation("分页查询材料对账单列表")
    public ResultData<PageResult<ReceiptReconciliationListVO>> page(@RequestBody ReconciliationQueryVO queryVO){
        return ResultData.data(reconciliationService.page(queryVO));
    }

    /**
     * 根据对账单id查询材料对账单详情列表
     */
    @GetMapping("/detail")
    @ApiOperation("查询材料对账单详情列表")
    public ResultData<ReceiptReconciliationDetailPageVO> getDetail(@RequestParam("id") Long id){
        return ResultData.data(reconciliationService.getDetail(id));
    }
}