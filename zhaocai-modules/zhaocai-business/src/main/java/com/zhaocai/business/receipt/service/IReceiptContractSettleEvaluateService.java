package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleEvaluate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleEvaluateVO;

/**
 * 供应商评价 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleEvaluateService extends IService<ReceiptContractSettleEvaluate> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleEvaluateVO> selectList(ReceiptContractSettleEvaluateVO pushPurchase);
    /**
     * 新增推送结算单
     *
     * @param pushContractSettleEvaluateList 新增推送结算单明细
     * @return 结果
     */
    public boolean insertPushContractSettleEvaluateSaveBatch(List<ReceiptContractSettleEvaluate> pushContractSettleEvaluateList);
}
