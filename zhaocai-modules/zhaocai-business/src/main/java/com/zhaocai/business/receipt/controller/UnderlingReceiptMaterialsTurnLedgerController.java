package com.zhaocai.business.receipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerVO;
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
@RequestMapping("/underling/materialsTurnLedger")
public class UnderlingReceiptMaterialsTurnLedgerController extends BladeController {


    @Autowired
    private IReceiptMaterialsTurnLedgerService turnLedgerService;

    /**
     * 接收租赁周材台账
     */
    @PostMapping("/receiptMaterialsTurnLedger")
    @Log(title = "租赁周材台账", businessType = BusinessType.INSERT)
    public ResultData receiptMaterialsTurnLedger(@RequestBody ReceiptMaterialsTurnLedgerVO turnLedgerVO){
        // 将RequestBody转换为JSON对象
        log.info("租赁周材台账数据：" + JacksonUtil.toJsonString(turnLedgerVO));
        // 打印JSON对象
        System.out.println("租赁周材台账数据：" +JacksonUtil.toJsonString(turnLedgerVO));
        return ResultData.data(turnLedgerService.insertMaterialsTurnLedger(turnLedgerVO));
    }
}
