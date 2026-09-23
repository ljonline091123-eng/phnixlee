package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleDeposit;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleDepositVO;

/**
 * 押金、保证金信息 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleDepositService extends IService<ReceiptContractSettleDeposit> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleDepositVO> selectList(ReceiptContractSettleDepositVO pushPurchase);

    /**
     * 新增推送结算单
     *
     * @param settleDepositList 新增推送结算单明细
     * @return 结果
     */
    public boolean insertPushContractSettleDepositSaveBatch(List<ReceiptContractSettleDeposit> settleDepositList);
}
