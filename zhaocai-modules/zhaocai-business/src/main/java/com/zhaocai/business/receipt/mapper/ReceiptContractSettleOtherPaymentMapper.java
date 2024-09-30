package com.zhaocai.business.receipt.mapper;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleOtherPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleOtherPaymentVO;

import java.util.List;

/**
 * 推送结算单 Mapper接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface ReceiptContractSettleOtherPaymentMapper extends BaseMapper<ReceiptContractSettleOtherPayment> {
    /**
     * 查询结算单
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleOtherPaymentVO> selectList(ReceiptContractSettleOtherPaymentVO pushPurchase);
}
