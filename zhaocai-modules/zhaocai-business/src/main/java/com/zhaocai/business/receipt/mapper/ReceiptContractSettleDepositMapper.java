package com.zhaocai.business.receipt.mapper;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleDeposit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleDepositVO;

import java.util.List;

/**
 * 推送结算单 Mapper接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface ReceiptContractSettleDepositMapper extends BaseMapper<ReceiptContractSettleDeposit> {
    /**
     * 查询结算单
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleDepositVO> selectList(ReceiptContractSettleDepositVO pushPurchase);
}
