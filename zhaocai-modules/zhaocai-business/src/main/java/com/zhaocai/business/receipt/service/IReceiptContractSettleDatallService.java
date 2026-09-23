package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleDatall;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleDatallVO;

/**
 * 计日工明细 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleDatallService extends IService<ReceiptContractSettleDatall> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleDatallVO> selectList(ReceiptContractSettleDatallVO pushPurchase);

    /**
     * 新增推送结算单
     *
     * @param pushContractSettleDatallList 新增推送结算单明细
     * @return 结果
     */
    public boolean insertPushContractSettleDatallSaveBatch(List<ReceiptContractSettleDatall> pushContractSettleDatallList);

}
