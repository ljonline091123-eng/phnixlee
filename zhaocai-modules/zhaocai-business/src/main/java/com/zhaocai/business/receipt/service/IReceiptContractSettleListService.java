package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleList;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleListVO;

/**
 * 清单结算明细 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleListService extends IService<ReceiptContractSettleList> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleListVO> selectList(ReceiptContractSettleListVO pushPurchase);

    /**
     * 新增推送结算单
     *
     * @param pushContractSettleLists 新增推送结算单明细
     * @return 结果
     */
    public boolean insertPushContractSettleListSaveBatch(List<ReceiptContractSettleList> pushContractSettleLists);

}
