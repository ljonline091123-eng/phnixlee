package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerDtlDtlService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlDtlVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsTurnLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerVO;
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
 * 租赁周材台账Controller
 *
 * @author cff
 * @date 2024-09-06
 */
@Api(value = "第三方-租赁周材台账接口")
@Slf4j
@RestController
@RequestMapping("/materialsTurnLedger")
public class ReceiptMaterialsTurnLedgerController extends BladeController {


    @Autowired
    private IReceiptMaterialsTurnLedgerService turnLedgerService;

    @Autowired
    private IReceiptMaterialsTurnLedgerDtlDtlService turnLedgerDtlDtlService;

    /**
     * 租赁周材台账列表
     */
    @GetMapping("/materialsTurnLedgerListPage")
    @ApiOperation(value = "租赁周材台账列表")
    public ResultData<PageResult<ReceiptMaterialsTurnLedgerVO>> materialsTurnLedgerListPage(ReceiptMaterialsTurnLedgerQueryVO turnLedgerQueryVO)
    {
        return ResultData.data(turnLedgerService.materialsTurnLedgerListPage(turnLedgerQueryVO));
    }

    /**
     * 租赁周材台账 详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation(value = "根据id查询租赁周材台账详细信息")
    public ResultData<ReceiptMaterialsTurnLedgerVO> getMaterialsTurnLedgerInfo(@PathVariable("id") Long id) {
        return ResultData.data(turnLedgerService.detail(id));
    }


    /**
     * 租赁周材台账弹框 详细信息
     */
    @GetMapping(value = "/detail/{id}")
    @ApiOperation(value = "根据id查询租赁周材台账弹框详细信息")
    public ResultData<List<ReceiptMaterialsTurnLedgerDtlDtlVO>> getReceiptMaterialsTurnLedgerDtlDtlList(@PathVariable("id") Long id) {
        return ResultData.data(turnLedgerDtlDtlService.getReceiptMaterialsTurnLedgerDtlDtlList(id));
    }
}
