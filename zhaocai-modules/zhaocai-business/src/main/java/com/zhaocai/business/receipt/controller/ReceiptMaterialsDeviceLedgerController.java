package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerDtlDtlService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlDtlVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsDeviceLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备租赁台账Controller
 *
 * @author cff
 * @date 2024-09-11
 */
@Api(value = "第三方-设备租赁台账接口")
@Slf4j
@RestController
@RequestMapping("/materialsDeviceLedger")
public class ReceiptMaterialsDeviceLedgerController extends BladeController {


    @Autowired
    private IReceiptMaterialsDeviceLedgerService deviceLedgerService;

    @Autowired
    private IReceiptMaterialsDeviceLedgerDtlDtlService deviceLedgerDtlDtlService;

    /**
     * 设备租赁台账列表
     */
    @GetMapping("/materialsDeviceLedgerListPage")
    @ApiOperation(value = "设备租赁台账列表")
    public ResultData<PageResult<ReceiptMaterialsDeviceLedgerVO>> materialsDeviceLedgerListPage(ReceiptMaterialsDeviceLedgerQueryVO receiptPurchaseVO)
    {
        return ResultData.data(deviceLedgerService.materialsDeviceLedgerListPage(receiptPurchaseVO));
    }

    /**
     * 设备租赁台账 详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation(value = "根据id查询设备租赁台账详细信息")
    public ResultData<ReceiptMaterialsDeviceLedgerVO> getMaterialsDeviceLedgerInfo(@PathVariable("id") Long id)
    {
        return ResultData.data(deviceLedgerService.detail(id));
    }


    /**
     * 设备租赁台账弹框 详细信息
     */
    @GetMapping(value = "/detail/{id}")
    @ApiOperation(value = "根据id查询设备租赁台账弹框细信息")
    public ResultData<List<ReceiptMaterialsDeviceLedgerDtlDtlVO>> getMaterialsDeviceLedgerDtlDtlList(@PathVariable("id") Long id) {
        return ResultData.data(deviceLedgerDtlDtlService.getMaterialsDeviceLedgerDtlDtlList(id));
    }

}
