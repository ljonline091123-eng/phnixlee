package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleSporadicWork;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleSporadicWorkVO;

/**
 * 推送结算单 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleSporadicWorkService extends IService<ReceiptContractSettleSporadicWork> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleSporadicWorkVO> selectList(ReceiptContractSettleSporadicWorkVO pushPurchase);
    /**
     * 新增推送结算单
     *
     * @param sporadicWorksList 新增推送结算单明细
     * @return 结果
     */
    public boolean insertSporadicWorksSaveBatch(List<ReceiptContractSettleSporadicWork> sporadicWorksList);

}
