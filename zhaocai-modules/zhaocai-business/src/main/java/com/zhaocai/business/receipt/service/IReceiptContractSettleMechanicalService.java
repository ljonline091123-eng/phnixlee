package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleMechanical;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleMechanicalVO;

/**
 * 机械台班明细 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettleMechanicalService extends IService<ReceiptContractSettleMechanical> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleMechanicalVO> selectList(ReceiptContractSettleMechanicalVO pushPurchase);

    /**
     * 新增推送结算单
     *
     * @param pushContractSettleMechanicalList 新增推送结算单明细
     * @return 结果
     */
    public boolean insertPushContractSettleMechanicalSaveBatch(List<ReceiptContractSettleMechanical> pushContractSettleMechanicalList);

}
