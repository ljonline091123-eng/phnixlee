package com.zhaocai.business.receipt.service;

import com.zhaocai.business.receipt.vo.res.ReceiptThirdRequestVO;
import com.zhaocai.business.receipt.vo.res.ReceiptThirdResponseVO;

/**
 * 推送数据 Service接口
 *
 * @author cff
 * @date 2024-09-11
 */
public interface IPushReceiptService {

    /**
     * 推送采购订单状态
     * @param deviceLedgerVO
     * @return boolean
     */
    boolean modifyPurchaseState(ReceiptThirdRequestVO deviceLedgerVO);

    /**
     * 推送结算单状态
     * @param deviceLedgerVO
     * @return boolean
     */
    boolean modifyContractsettlementState(ReceiptThirdRequestVO deviceLedgerVO);

    /**
     * 推送设备租赁台账状态
     * @param deviceLedgerVO
     * @return boolean
     */
    boolean modifyDeviceLedgerState(ReceiptThirdRequestVO deviceLedgerVO);


    /**
     * 推送租赁周材台账账状态
     * @param deviceLedgerVO
     * @return boolean
     */
    boolean modifyTurnLedgerState(ReceiptThirdRequestVO deviceLedgerVO);

    /**
     * 推送材料对账单供应商状态
     * @param deviceLedgerVO 接收参数对象VO
     * @return boolean
     */
    boolean reconciliationState(ReceiptThirdRequestVO deviceLedgerVO);
}
