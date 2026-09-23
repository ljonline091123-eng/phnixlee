package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleOtherPayment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleOtherPaymentVO;

/**
 * 推送结算单 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleOtherPaymentService extends IService<ReceiptContractSettleOtherPayment> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleOtherPaymentVO> selectList(ReceiptContractSettleOtherPaymentVO pushPurchase);

    /**
     * 新增推送结算单
     *
     * @param otherPaymentList 新增推送结算单明细
     * @return 结果
     */
    public boolean insertOtherPaymentListSaveBatch(List<ReceiptContractSettleOtherPayment> otherPaymentList);


}
