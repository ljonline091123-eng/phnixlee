package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.service.UnderlingRestTemplateService;
import com.zhaocai.business.receipt.domain.ReceiptReconciliation;
import com.zhaocai.business.receipt.service.IPushReceiptService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerService;
import com.zhaocai.business.receipt.service.IReceiptReconciliationService;
import com.zhaocai.business.receipt.service.*;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettlementVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.business.receipt.vo.res.ReceiptThirdRequestVO;
import com.zhaocai.business.receipt.vo.res.ReceiptThirdResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 推送单据状态 Service业务层处理
 *
 * @author cff
 * @date 2024-09-11
 */
@Service
public class PushReceiptServiceImpl implements IPushReceiptService {

    @Autowired
    private IReceiptMaterialsDeviceLedgerService deviceLedgerService;

    @Autowired
    private IReceiptMaterialsTurnLedgerService turnLedgerService;

    @Autowired
    private IReceiptReconciliationService reconciliationService;

    @Autowired
    private IReceiptContractSettlementService settlementService;

    @Autowired
    private IReceiptPurchaseService purchaseService;

    /**
     *
     * @param deviceLedgerVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modifyPurchaseState(ReceiptThirdRequestVO deviceLedgerVO) {
        ReceiptPurchaseVO purchaseVO=new ReceiptPurchaseVO();
        purchaseVO.setThirdId(deviceLedgerVO.getId());
        purchaseVO.setState(deviceLedgerVO.getSupplierStatus());
        /*UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.LEASE_WRITE_BACK_SUPPLIER_STATUS,
                ReceiptThirdResponseVO.class, deviceLedgerVO);*/
        return purchaseService.modifyPurchaseState(purchaseVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modifyContractsettlementState(ReceiptThirdRequestVO deviceLedgerVO) {
        ReceiptContractSettlementVO receiptContractSettlementVO=new ReceiptContractSettlementVO();
        receiptContractSettlementVO.setThirdId(deviceLedgerVO.getId());
        receiptContractSettlementVO.setState(deviceLedgerVO.getSupplierStatus());
        /*UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.LEASE_WRITE_BACK_SUPPLIER_STATUS,
                ReceiptThirdResponseVO.class, deviceLedgerVO);*/
        return settlementService.modifyContractsettlementState(receiptContractSettlementVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modifyDeviceLedgerState(ReceiptThirdRequestVO deviceLedgerVO) {
        ReceiptMaterialsDeviceLedgerVO materialsDeviceLedgerVO=new ReceiptMaterialsDeviceLedgerVO();
        materialsDeviceLedgerVO.setThirdId(deviceLedgerVO.getId());
        materialsDeviceLedgerVO.setState(deviceLedgerVO.getSupplierStatus());
//        UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.LEASE_WRITE_BACK_SUPPLIER_STATUS,
//                ReceiptThirdResponseVO.class, deviceLedgerVO);

        return deviceLedgerService.modifyDeviceLedgerState(materialsDeviceLedgerVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modifyTurnLedgerState(ReceiptThirdRequestVO deviceLedgerVO) {
        ReceiptMaterialsTurnLedgerVO turnLedgerVO=new ReceiptMaterialsTurnLedgerVO();
        turnLedgerVO.setThirdId(deviceLedgerVO.getId());
        turnLedgerVO.setState(deviceLedgerVO.getSupplierStatus());
//        UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.TURNLEDGER_WRITE_BACK_SUPPLIER_STATUS,
//                ReceiptThirdResponseVO.class,deviceLedgerVO);
        return turnLedgerService.modifyMaterialsTurnLedgerState(turnLedgerVO);
    }

    @Override
    public boolean reconciliationState(ReceiptThirdRequestVO deviceLedgerVO) {
        String id = deviceLedgerVO.getId();
        String supplierStatus = deviceLedgerVO.getSupplierStatus();
        //推送供应商状态至成控
//        UnderlingRestTemplateService.postForObject(
//                UnderlingPlatformUrlEnum.RECONCILIATION_WRITE_BACK_SUPPLIER_STATUS,
//                ReceiptThirdResponseVO.class,deviceLedgerVO);
        //修改材料对账单中供应商状态
        return reconciliationService.update(
                new LambdaUpdateWrapper<ReceiptReconciliation>()
                        .eq(ReceiptReconciliation::getThirdId,id)
                        .set(ReceiptReconciliation::getSupplierStatus,supplierStatus));
    }
}
