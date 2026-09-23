package com.zhaocai.business.receipt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseDetail;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseDetailVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 推送采购订单明细 Service接口
 *
 * @author WH
 * @date 2024-09-06
 */
public interface IReceiptPurchaseDetailService extends IService<ReceiptPurchaseDetail> {


    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseDetailVO> selectList(ReceiptPurchaseDetailVO pushPurchase);

    /**
     * 根据id查出当前采购订单可下订单数量总和
     * @param parntId
     * @return
     */
    int purchaseDetailSum(String parntId);

    /**
     * 新增推送采购订单
     *
     * @param purchaseDetailList 新增推送采购订单明细
     * @return 结果
     */
    public boolean insertPushPurchaseSaveBatch(List<ReceiptPurchaseDetail> purchaseDetailList);

    /**
     * 修改采购订单明细
     *
     * @param purchaseDetailList 新增推送采购订单明细
     * @return 结果
     */
    public boolean updatePushPurchaseBatch(List<ReceiptPurchaseDetail> purchaseDetailList);
}
